package org.ipring.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
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
import org.ipring.util.StringMatchUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.ipring.model.gemini.ImportExcelVO.SignType.COMMON;

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

    @PostMapping("/azure-ai")
    @StlApiOperation(title = "azure-ai", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ChatCompletions> azureAiGpt(@RequestBody ChatBody chatBody) {
        return ReturnFactory.success(openAIService.getImageResp(chatBody));
    }


    @PostMapping("/4o-mini")
    @StlApiOperation(title = "40-mini", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
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

    @PostMapping("/4o-mini/textMap")
    @StlApiOperation(title = "4o-mini 测试一次，返回自定义模型", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
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

    @PostMapping("/4o-mini/import-choice-one")
    @StlApiOperation(title = "4o-mini 导入测试一条数据", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ImportExcelVO> importExcelTestOne(@RequestParam Integer index, @RequestParam(required = false) String model, @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        ImportExcelVO pod = podList.get(index);
        ImportExcelVO.SignType signType = ImportExcelVO.SignType.all_map.getOrDefault(pod.getSignType(), COMMON);
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
    public void importExcel(@RequestParam(name = "model", required = false) String model, @RequestParam(required = false) Integer supplier, @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        log.info("图像识别元数据，总计{}条", podList.size());
        int i = 0;
        for (ImportExcelVO pod : podList) {
            // ImportExcelVO.SignType signType = ImportExcelVO.SignType.all_map.getOrDefault(pod.getSignType(), ImportExcelVO.SignType.COMMON);
            ImportExcelVO.SignType signType = ImportExcelVO.SignType.COMMON;
            ChatBody chatBody = new ChatBody();
            chatBody.setModel(model);
            chatBody.setSupplier(supplier);
            chatBody.setImageList(pod.getImgToImgList());

            chatBody.setSystemSetup(signType.getSystemSetup());
            String question = String.format(signType.getQuestion(), pod.getWaybillNo().length(), StringUtils.substring(pod.getWaybillNo(), 0, 6));
            chatBody.setText(question);
            pod.setQuestion(question);
            try {
                i++;
                long start = System.currentTimeMillis();
                log.info("第{}条，开始调用：{}", i, JsonUtils.toJson(chatBody));
                // Return<BigModelAnswerText> textMap = this.getTextMap(chatBody);
                Return<ChatCompletions> textMap = this.azureAiGpt(chatBody);
                // 耗时
                long spendTime = System.currentTimeMillis() - start;
                log.info("第{}条，AI识别完成，耗时：{}ms", i, spendTime);
                if (textMap.success() && textMap.hashData()) {
                    ChatCompletions chatCompletions = textMap.getBodyMessage();

                    List<Questionnaire> questionnairesList = chatCompletions.getChoices().stream().map(choice -> JsonUtils.toObject(choice.getMessage().getContent(), Questionnaire.class))
                            .collect(Collectors.toList());
                    if (CollectionUtil.isNotEmpty(questionnairesList) && questionnairesList.size() == 1) {
                        pod.setAnswer(chatCompletions.getChoices().get(0).getMessage().getContent());
                        Questionnaire questionnaire = questionnairesList.get(0);
                        BeanUtil.copyProperties(questionnaire, pod);
                        pod.setMatchingRate(StringMatchUtils.matchingRate(questionnaire.getQ2(), pod.getWaybillNo()));
                    }
                    pod.setUsageMetadata(JsonUtils.toJson(chatCompletions.getUsage()));
                    pod.setModel(chatCompletions.getModel());
                    pod.setTime(spendTime);
                } else {
                    log.error("第{}条，识别异常，跳过", i);
                    continue;
                }
            } catch (Exception e) {
                log.error("第{}条 远程异常：", i, e);
                pod.setAnswer("调用异常->" + e.getLocalizedMessage());
                sleep(5); // 控制速率
                continue;
            }
            // 控制速率
            // sleep(1);
        }
        String answerAll = podList.stream().map(ImportExcelVO::getAnswer).collect(Collectors.joining(","));
        log.info("识别结束，开始写入本地文件：{}", answerAll);
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        // ExcelOperateUtils.downData(sxssfWorkbook, response, "pod.xlsx");
        String fileName = writeLocalPath("gpt", sxssfWorkbook);
        log.info("识别结束，写入本地文件成功 {}", fileName);
    }

    private static void sleep(Integer seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            log.error("睡眠，抛出异常：", e);
        }
    }

    private static String writeLocalPath(String namePrefix, SXSSFWorkbook workbook) {
        String name = namePrefix + "pod" + System.currentTimeMillis();
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
}
