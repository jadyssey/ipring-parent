package org.ipring.controller;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.gateway.GeminiGateway;
import org.ipring.model.BigModelAnswerText;
import org.ipring.model.ChatBody;
import org.ipring.model.GeminiResponse;
import org.ipring.model.ResponseMapper;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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
        GeminiResponse geminiResponse = ResponseMapper.mapGenerateContentResponseToGeminiResponse(generateContentResponse);
        List<String> textList = ResponseMapper.extractTextFromGeminiResponse(geminiResponse);
        // 转响应模型
        BigModelAnswerText resp = new BigModelAnswerText();
        resp.setSourceTextList(textList);
        resp.setUsageMetadata(geminiResponse.getUsageMetadata());
        return ReturnFactory.success(resp);
    }


}
