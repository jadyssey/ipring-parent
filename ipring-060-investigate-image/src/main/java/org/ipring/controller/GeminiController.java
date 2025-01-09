package org.ipring.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * @author liuguangjin
 * @date 2025/1/8
 */
@Api(tags = "校验测试接口")
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
        resp.setUsageMetadata(geminiResponse.getUsageMetadata());
        return ReturnFactory.success(resp);
    }

    @PostMapping("/gemini/import")
    @StlApiOperation(title = "gemini model import", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<List<ImportExcelVO>> importExcel(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        for (ImportExcelVO pod : podList) {
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

                String question = String.format(signType.getQuestion(), pod.getWaybillNo());
                chatBody.setText(question);
                pod.setQuestion(question);

                try {
                    Return<BigModelAnswerText> textMap = this.getTextMap(chatBody);
                    if (textMap.success()) {
                        BigModelAnswerText bodyMessage = textMap.getBodyMessage();
                        if (CollectionUtil.isNotEmpty(bodyMessage.getSourceTextList()))
                            pod.setAnswer(String.join(",", bodyMessage.getSourceTextList()));
                    } else {
                        break;
                    }
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        ExcelOperateUtils.downData(sxssfWorkbook, response, "pod识别结果.xlsx");
        return ReturnFactory.success(podList);
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

            String question = String.format(signType.getQuestion(), pod.getWaybillNo());
            chatBody.setText(question);
            pod.setQuestion(question);

            Return<BigModelAnswerText> textMap = this.getTextMap(chatBody);
            if (textMap.success()) {
                BigModelAnswerText bodyMessage = textMap.getBodyMessage();
                if (CollectionUtil.isNotEmpty(bodyMessage.getSourceTextList()))
                    pod.setAnswer(String.join(",", bodyMessage.getSourceTextList()));
                pod.setUsageMetadata(JsonUtils.toJson(bodyMessage.getUsageMetadata()));
            }
        }
        return ReturnFactory.success(pod);
    }
}
