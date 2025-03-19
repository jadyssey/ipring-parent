package org.ipring.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.azure.ai.openai.models.ChatCompletions;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.gateway.ChatGptGateway;
import org.ipring.gateway.ZhiPuAiGatewayImpl;
import org.ipring.gateway.azure.OpenAIService;
import org.ipring.model.BigModelAnswerText;
import org.ipring.model.ChatBody;
import org.ipring.model.ImageExplanationRequest;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.model.delivery.Questionnaire;
import org.ipring.model.gemini.ImportExcelVO;
import org.ipring.model.gpt.ChatGPTResponse;
import org.ipring.util.JsonUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Slf4j
@Api(tags = "chatgpt接口")
@RequestMapping("/chat")
@RestController
@Validated
@RequiredArgsConstructor
public class GPTController {
    private final ChatGptGateway chatGptGateway;
    @Resource
    private OpenAIService openAIService;

    @Resource
    private ThreadPoolTaskExecutor commonThreadPool;

    @PostMapping("/azure-ai")
    @StlApiOperation(title = "azure-ai", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ChatCompletions> azureAiGpt(@RequestBody ChatBody chatBody) {
        return ReturnFactory.success(openAIService.getImageResp(chatBody));
    }


//    @PostMapping("/4o-mini")
//    @StlApiOperation(title = "40-mini", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ChatGPTResponse> get(@RequestBody ChatBody chatBody) {
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setModel(Optional.ofNullable(chatBody.getModel()).orElse("gpt-4o-mini"));

        List<ChatMessage> messages = new ArrayList<>();
        List<ImageExplanationRequest> requests = new ArrayList<>();
        if (StringUtils.isNotBlank(chatBody.getImageUrl())) {
            requests.add(ZhiPuAiGatewayImpl.getImageChatContent(chatBody.getImageUrl()));
        }
        if (CollectionUtil.isNotEmpty(chatBody.getImageList())) {
            requests.addAll(chatBody.getImageList().stream().map(ZhiPuAiGatewayImpl::getImageChatContent).collect(Collectors.toList()));
        }
        requests.add(ZhiPuAiGatewayImpl.getTextChatContent(chatBody.getText()));
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), requests);
        messages.add(chatMessage);
        chatCompletionRequest.setMessages(messages);
        if (Objects.nonNull(chatBody.getSupplier()) && chatBody.getSupplier().equals(1)) {
            return chatGptGateway.completions(chatCompletionRequest);
        }
        return chatGptGateway.azureCompletions(chatCompletionRequest);
    }

//    @PostMapping("/4o-mini/textMap")
//    @StlApiOperation(title = "4o-mini 测试一次，返回自定义模型", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<BigModelAnswerText> getTextMap(@RequestBody ChatBody chatBody) {
        Return<ChatGPTResponse> res = this.get(chatBody);
        if (!res.success())
            return ReturnFactory.info(SystemServiceCode.SystemApi.WAIT_MIN);
        ChatGPTResponse chatGPTResponse = res.getBodyMessage();
        if (Objects.isNull(chatGPTResponse)) {
            return ReturnFactory.info(SystemServiceCode.SystemApi.WAIT_MIN);
        }
        BigModelAnswerText resp = new BigModelAnswerText();

        List<ChatGPTResponse.Choice> choices = chatGPTResponse.getChoices();
        List<String> sourceTextList = choices.stream().map(ChatGPTResponse.Choice::getMessage).map(ChatGPTResponse.Message::getContent).collect(Collectors.toList());
        resp.setSourceTextList(sourceTextList);
        resp.setGptUsage(chatGPTResponse.getUsage());
        resp.setModel(chatGPTResponse.getModel());
        return ReturnFactory.success(resp);
    }

//    @PostMapping("/4o-mini/import-choice-one")
//    @StlApiOperation(title = "4o-mini 导入测试一条数据", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ImportExcelVO> importExcelTestOne(@RequestParam Integer index, @RequestParam(required = false) String model, @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        ImportExcelVO pod = podList.get(index);
        ImportExcelVO.SignType signType = ImportExcelVO.SignType.all_map.getOrDefault(pod.getSignType(), ImportExcelVO.SignType.COMMON);
        if (Objects.nonNull(signType)) {
            ChatBody chatBody = new ChatBody();
            chatBody.setModel(model);
            List<String> imageList = new ArrayList<>();
            if (StringUtils.isNotBlank(pod.getImage1())) {
                imageList.add(pod.getImage1());
            }
            if (StringUtils.isNotBlank(pod.getImage2())) {
                imageList.add(pod.getImage2());
            }
            if (StringUtils.isNotBlank(pod.getImage3())) {
                imageList.add(pod.getImage3());
            }
            chatBody.setImageList(imageList);

            String question = String.format(signType.getQuestion(), StringUtils.substring(pod.getWaybillNo(), 0, 8));
            chatBody.setText(question);
            pod.setQuestion(question);

            Return<BigModelAnswerText> textMap = this.getTextMap(chatBody);
            if (textMap.success()) {
                BigModelAnswerText bodyMessage = textMap.getBodyMessage();
                if (CollectionUtil.isNotEmpty(bodyMessage.getSourceTextList()))
                    pod.setAnswer(String.join(",", bodyMessage.getSourceTextList()));
                pod.setUsageMetadata(JsonUtils.toJson(bodyMessage.getGptUsage()));
                pod.setModel(bodyMessage.getModel());
            }
        }
        return ReturnFactory.success(pod);
    }

    @PostMapping("/4o-mini/import")
    @StlApiOperation(title = "4o-mini 导入数据批量调用", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void importExcel(@RequestParam(name = "model", required = false) String model, @RequestParam(required = false) Integer supplier, @RequestParam("file") MultipartFile file, @RequestParam String fileName, HttpServletResponse response) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        long start = System.currentTimeMillis();
        log.info("图像识别元数据，总计{}条", podList.size());
        List<Future<?>> submitList = new ArrayList<>();
        for (int i = 0; i < podList.size(); i++) {
            ImportExcelVO pod = podList.get(i);
            int finalI = i;
            Future<?> submit = commonThreadPool.submit(() -> imageHandle(model, supplier, pod, finalI));
            submitList.add(submit);
        }
        try {
            for (Future<?> future : submitList) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("多线程报错，", e);
        }
        String answerAll = podList.stream().map(ImportExcelVO::getAnswer).collect(Collectors.joining("##"));
        log.info("识别结束，开始写入本地文件：{}", answerAll);
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        String fileNameResp = writeLocalPath("gpt_" + fileName, sxssfWorkbook);
        log.info("识别结束，写入本地文件成功 {}, 耗时：{}", fileNameResp, System.currentTimeMillis() - start);
    }

    private void imageHandle(String model, Integer supplier, ImportExcelVO pod, int i) {
        // ImportExcelVO.SignType signType = ImportExcelVO.SignType.all_map.getOrDefault(pod.getSignType(), ImportExcelVO.SignType.COMMON);
        ImportExcelVO.SignType signType = ImportExcelVO.SignType.Q_0221;
        ChatBody chatBody = new ChatBody();
        chatBody.setModel(model);
        chatBody.setSupplier(supplier);
        List<String> imgList = pod.getImgToImgList().stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        Collections.reverse(imgList);
        chatBody.setImageList(imgList);

        chatBody.setSystemSetup(signType.getSystemSetup());
        // String question = String.format(signType.getQuestion(), (pod.getAddress().replace(" ", "").length()/3) * 2);
        String question = signType.getQuestion().replace("%s", String.valueOf(pod.getWaybillNo().length()));
        chatBody.setText(question);
        chatBody.setJsonResponseFormat(signType.getJsonResponseFormat());
        pod.setQuestion(question);
        try {
            i++;
            log.info("第{}条，开始调用：{}", i, JsonUtils.toJson(chatBody));
            // Return<BigModelAnswerText> textMap = this.getTextMap(chatBody);
            // 前置处理二维码，由于网速不快，先放AI后置处理 todo
            /*pod.setQrCodeFlag("FALSE");
            long start = System.currentTimeMillis();
            for (String imgUrl : chatBody.getImageList()) {
                try {
                    String decodeWaybillNo = QRCodeUtil.decodeQRCodeFromURL(imgUrl);
                    if (pod.getWaybillNo().equalsIgnoreCase(decodeWaybillNo)) {
                        pod.setQrCodeUrl(imgUrl);
                        pod.setQrCodeFlag("TRUE");
                        pod.setAnswer("成功识别二维码，跳过AI识别");
                        return;
                    }
                } catch (Exception e) {
                    // 记录异常日志，可以根据实际需求调整处理方式
                    // 未识别到二维码也会到这里来
                    // log.error("Failed to decode QR code from URL: " + imgUrl, e);
                }
            }
            long spendTime = System.currentTimeMillis() - start;
            log.info("第{}条，二维码识别完成，耗时：{}ms", i, spendTime);*/
            long start = System.currentTimeMillis();
            Return<ChatCompletions> textMap = this.azureAiGpt(chatBody);
            // 记录耗时
            long spendTime = System.currentTimeMillis() - start;
            log.info("第{}条，AI识别完成，耗时：{}ms", i, spendTime);
            if (!textMap.success() || !textMap.hashData()) {
                log.error("第{}条，识别异常，跳过", i);
            }
            ChatCompletions chatCompletions = textMap.getBodyMessage();

            List<Questionnaire> questionnairesList = chatCompletions.getChoices().stream().map(choice -> JsonUtils.toObject(choice.getMessage().getContent(), Questionnaire.class)).collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(questionnairesList) && questionnairesList.size() == 1) {
                pod.setAnswer(chatCompletions.getChoices().get(0).getMessage().getContent());
                Questionnaire questionnaire = questionnairesList.get(0);
                BeanUtil.copyProperties(questionnaire, pod);
                // pod.setMatchingRate(StringMatchUtils.matchingRate(questionnaire.getQ2(), pod.getWaybillNo()));

                // 后置二维码识别 todo
                pod.setQrCodeFlag("-");
                if (!pod.getQ1().equalsIgnoreCase("TRUE")) {
                    log.info("第{}条，二维码识别开始", i);
                    for (String imgUrl : chatBody.getImageList()) {
                        try {
                            BufferedImage bufferedImage = ImageIO.read(new URL(imgUrl));
                            String decodeWaybillNo = QrCodeUtil.decode(bufferedImage);
                            if (pod.getWaybillNo().equalsIgnoreCase(decodeWaybillNo)) {
                                pod.setQrCodeUrl(imgUrl);
                                pod.setQrCodeFlag("TRUE");
                                break;
                                // pod.setAnswer("成功识别二维码，跳过AI识别");
                            } else if (StringUtils.isNotBlank(decodeWaybillNo)) {
                                pod.setQrCodeFlag("FALSE");
                                // 查出数据了，但是与运单不一致
                                pod.setQrCodeUrl(decodeWaybillNo + "#" + imgUrl);
                            } else {
                                pod.setQrCodeFlag("FALSE");
                            }
                        } catch (Exception e) {
                            pod.setQrCodeFlag("ERROR");
                            // 记录异常日志，可以根据实际需求调整处理方式
                            log.error("Failed to decode QR code from URL: " + imgUrl, e);
                        }
                    }
                }
            }
            pod.setUsageMetadata(JsonUtils.toJson(chatCompletions.getUsage()));
            pod.setModel(chatCompletions.getModel());
            pod.setTime(spendTime);
        } catch (Exception e) {
            log.error("第{}条 远程异常：", i, e);
            pod.setAnswer("调用异常->" + e.getLocalizedMessage());
            // sleep(5); // 控制速率
        }
    }

    private static void sleep(Integer seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            log.error("睡眠，抛出异常：", e);
        }
    }

    private static String writeLocalPath(String namePrefix, SXSSFWorkbook workbook) {
        String name = namePrefix + System.currentTimeMillis();
        try (FileOutputStream outputStream = new FileOutputStream(name + ".xlsx")) {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭 SXSSFWorkbook，释放资源
            try {
                workbook.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return name;
    }

    public static void main(String[] args) throws IOException {
        String imgUrl = "https://gofo-sys-admin.s3.us-west-2.amazonaws.com/sys-mod-file/2025-02-24/app-file/17404443849090EA9D875-D9A0-4292-9A0D-6B27FE8580FE-966-000000F7A5A5EDBB.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250225T124655Z&X-Amz-SignedHeaders=host&X-Amz-Expires=604800&X-Amz-Credential=AKIAR234HW752KIISC4O%2F20250225%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Signature=f799f29467253644bfbd530b17fb7faa80cc5f9e69f1188bf04a4baab138366c";
        BufferedImage bufferedImage = ImageIO.read(new URL(imgUrl));
        String decodeWaybillNo = QrCodeUtil.decode(bufferedImage);
        System.out.println("decodeWaybillNo = " + decodeWaybillNo);
    }
}
