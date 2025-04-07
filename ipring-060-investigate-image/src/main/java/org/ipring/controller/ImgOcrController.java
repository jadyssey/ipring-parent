package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.model.common.Return;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benjaminwan.ocrlibrary.OcrResult;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.ocr.config.ParamConfig;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
/**
 * 图像识别控制器
 *
 * @author liuguangjin
 * @date 2025/4/2
 **/
@Api(tags = "图像识别控制器")
@RequestMapping("/ocr")
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class ImgOcrController {

    @GetMapping()
    @StlApiOperation(title = "本地文件识别", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public String ocr(@RequestParam String imgName) {
        ParamConfig paramConfig = ParamConfig.getDefaultConfig();
        paramConfig.setDoAngle(false);
        paramConfig.setMostAngle(false);
        InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
        // 开始识别
        OcrResult ocrResult = engine.runOcr(getResourcePath(imgName), paramConfig);
        return ocrResult.getStrRes().toString();
    }
    private static String getResourcePath(String path) {
        return new File("D:\\img\\imgs\\" + path).toString();
    }

    @PostMapping
    @StlApiOperation(title = "上传文件识别", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public String ocr(@RequestParam("file") MultipartFile fileUpload) throws IOException {
        ParamConfig paramConfig = ParamConfig.getDefaultConfig();
        paramConfig.setDoAngle(true);
        paramConfig.setMostAngle(true);
        InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
        File file = new File(System.getProperty("java.io.tmpdir") + "ocrJava/test.png");
        fileUpload.transferTo(file);
        file.deleteOnExit();

        OcrResult ocrResult = engine.runOcr(file.getPath(),paramConfig);
        return ocrResult.getStrRes().toString();
    }
}
