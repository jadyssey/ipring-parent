package org.ipring.util.ocr;


import com.benjaminwan.ocrlibrary.OcrResult;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.ocr.config.ParamConfig;

import java.io.File;

public class TestRapidOcr {
    public static void main(String[] args) {
        ParamConfig paramConfig = ParamConfig.getDefaultConfig();
        paramConfig.setDoAngle(true);
        paramConfig.setMostAngle(true);
        String imgPath = getResourcePath("/1742435406222ckcap1328967862967132935.jpg");
        InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
        OcrResult ONNXResult = engine.runOcr(imgPath);
    }

    private static String getResourcePath(String path) {
        return new File("D:\\img\\imgs\\" + path).toString();
    }
}