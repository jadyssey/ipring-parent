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
        String imgPath = getResourcePath("/1744168184433ABB58ABA-690F-415B-A048-D5D7DE997CDF.jpg");
        InferenceEngine engine1 = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
        InferenceEngine engine2 = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
        OcrResult ONNXResult1 = engine1.runOcr(imgPath);
        OcrResult ONNXResult2 = engine2.runOcr(imgPath);
        System.out.println("ONNXResult1 = " + ONNXResult1.getStrRes());
        System.out.println("ONNXResult2 = " + ONNXResult2.getStrRes());
    }

    private static String getResourcePath(String path) {
        return new File("D:\\img\\imgs\\" + path).toString();
    }
}