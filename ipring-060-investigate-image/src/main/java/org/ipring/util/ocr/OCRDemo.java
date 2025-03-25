package org.ipring.util.ocr;

import lombok.SneakyThrows;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.ipring.util.WeChatQRCodeTool;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class OCRDemo {
    private static final String IMAGE_PATH = "D:\\img\\";

    @SneakyThrows
    public static void main(String[] args) {

        File imageFile = new File("D:\\img\\test-3.jpg");
        BufferedImage bufferedImage = ImageIO.read(imageFile);
        int cvtype = CvType.CV_8UC3;
        if (bufferedImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            cvtype = CvType.CV_8UC1;
        }
        Mat mat = WeChatQRCodeTool.bufImg2Mat(bufferedImage, bufferedImage.getType(), cvtype);
        Mat blob = Dnn.blobFromImage(mat, 1.0, new Size(320, 320),
                new Scalar(123.68, 116.78, 103.94), true, false);
        Net net = Dnn.readNet("D:\\git\\myCode\\ipring-parent\\ipring-060-investigate-image\\src\\main\\resources\\east\\frozen_east_text_detection.pb");
        net.setInput(blob);
        List<Mat> outs = new ArrayList<>();
        List<String> outBlobNames = Arrays.asList("feature_fusion/Conv_7/Sigmoid", "feature_fusion/concat_3");
        net.forward(outs, outBlobNames);
        // 处理检测结果
        List<Rect> boxes = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        for (int i = 0; i < outs.get(0).rows(); i++) {
            Mat prob = outs.get(0).row(i);
            Mat rect = outs.get(1).row(i);
            // 解析检测结果，提取文本区域
            // ...
        }

        ITesseract instance = new Tesseract();

        // 动态获取资源路径（适用于开发环境）
        instance.setDatapath("D:\\git\\myCode\\ipring-parent\\ipring-060-investigate-image\\src\\main\\resources\\tessdata");

        instance.setLanguage("eng"); // 切换为英文识别
        try {
             String result = instance.doOCR(imageFile);

            BufferedImage buffImage = null;
            try {
                buffImage = ImageIO.read(imageFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<Word> words = instance.getWords(buffImage, 1);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }
}