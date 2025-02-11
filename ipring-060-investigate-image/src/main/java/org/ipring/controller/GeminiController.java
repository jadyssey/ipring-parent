package org.ipring.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.gateway.GeminiGateway;
import org.ipring.model.BigModelAnswerText;
import org.ipring.model.ChatBody;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.model.gemini.GeminiResponse;
import org.ipring.model.gemini.ImportExcelVO;
import org.ipring.model.gemini.ResponseMapper;
import org.ipring.util.JsonUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * @author liuguangjin
 * @date 2025/1/8
 */
@Slf4j
@Api(tags = "谷歌gemini接口")
@RequestMapping("/chat")
@RestController
@Validated
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiGateway geminiGateway;

    @PostMapping("/gemini")
    @StlApiOperation(title = "gemini model", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<GeminiResponse> get(@RequestBody ChatBody chatBody) {
        GenerateContentResponse generateContentResponse = geminiGateway.gemini15pro(chatBody);
        GeminiResponse geminiResponse = ResponseMapper.mapGenerateContentResponseToGeminiResponse(generateContentResponse);
        return ReturnFactory.success(geminiResponse);
    }

    @PostMapping("/gemini/textMap")
    @StlApiOperation(title = "gemini model textMap", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<BigModelAnswerText> getTextMap(@RequestBody ChatBody chatBody) {
        GenerateContentResponse generateContentResponse = geminiGateway.gemini15pro(chatBody);
        if (Objects.isNull(generateContentResponse))
            return ReturnFactory.info(SystemServiceCode.SystemApi.LOCK_FREQUENTLY_MIN);

        GeminiResponse geminiResponse = ResponseMapper.mapGenerateContentResponseToGeminiResponse(generateContentResponse);
        List<String> textList = ResponseMapper.extractTextFromGeminiResponse(geminiResponse);
        // 转响应模型
        BigModelAnswerText resp = new BigModelAnswerText();
        resp.setSourceTextList(textList);
        resp.setGeminiUsage(geminiResponse.getUsageMetadata());
        resp.setModel(geminiResponse.getUnknownFields().getFields().values().stream().map(JsonUtils::toJson).collect(Collectors.joining(",")));
        return ReturnFactory.success(resp);
    }

    @PostMapping("/gemini/import")
    @StlApiOperation(title = "gemini model import", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void importExcel(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        log.info("图像识别元数据，总计{}条", podList.size());
        int i = 0;
        for (ImportExcelVO pod : podList) {
            ImportExcelVO.SignType signType = ImportExcelVO.SignType.all_map.getOrDefault(pod.getSignType(), ImportExcelVO.SignType.COMMON);
            if (Objects.nonNull(signType)) {
                ChatBody chatBody = new ChatBody();
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

                try {
                    i++;
                    log.info("第{}条，开始调用：{}", i, JsonUtils.toJson(chatBody));
                    Return<BigModelAnswerText> textMap = this.getTextMap(chatBody);
                    log.info("第{}条，AI识别完成：{}", i, JsonUtils.toJson(textMap.getBodyMessage()));
                    if (textMap.success()) {
                        BigModelAnswerText bodyMessage = textMap.getBodyMessage();
                        if (CollectionUtil.isNotEmpty(bodyMessage.getSourceTextList()))
                            pod.setAnswer(String.join(",", bodyMessage.getSourceTextList()));
                        pod.setUsageMetadata(JsonUtils.toJson(bodyMessage.getGeminiUsage()));
                    } else {
                        break;
                    }
                    // TimeUnit.SECONDS.sleep(58);
                } catch (Exception e) {
                    break;
                }
            }
        }
        String answerAll = podList.stream().map(ImportExcelVO::getAnswer).collect(Collectors.joining(","));
        log.info("识别结束，开始写入本地文件：{}", answerAll);
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        // ExcelOperateUtils.downData(sxssfWorkbook, response, "pod.xlsx");
        String fileName = writeLocalPath(sxssfWorkbook);
        log.info("识别结束，写入本地文件成功 {}", fileName);
    }

    private static String writeLocalPath(SXSSFWorkbook workbook) {
        String name = "pod" + System.currentTimeMillis();
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

    @PostMapping("/gemini/test-one")
    @StlApiOperation(title = "gemini model import test-one by index", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ImportExcelVO> importExcelTestOne(@RequestParam Integer index, @RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        ImportExcelVO pod = podList.get(index);
        ImportExcelVO.SignType signType = ImportExcelVO.SignType.all_map.get(pod.getSignType());
        if (Objects.nonNull(signType)) {
            ChatBody chatBody = new ChatBody();
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
                pod.setUsageMetadata(JsonUtils.toJson(bodyMessage.getGeminiUsage()));
            }
        }
        return ReturnFactory.success(pod);
    }
}



