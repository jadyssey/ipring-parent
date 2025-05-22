package org.ipring.controller;

import com.benjaminwan.ocrlibrary.OcrResult;
import com.benjaminwan.ocrlibrary.TextBlock;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.ocr.config.ParamConfig;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.model.common.Return;
import org.ipring.model.gpt.ImportExcelV2VO;
import org.ipring.util.ImageDownloader;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * 图像识别控制器
 *
 * @author liuguangjin
 * @date 2025/4/2
 **/
@Api(tags = "图像OCR")
@RequestMapping("/ocr")
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class ImgOcrController {
    private final ThreadPoolTaskExecutor commonThreadPool;

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

    @PostMapping("/excel")
    @StlApiOperation(title = "pod ocr识别--模型直出", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void modelTempImportExcelAndExport(@RequestParam("file") MultipartFile file, HttpServletResponse response, @RequestParam String fileName) {
        List<ImportExcelV2VO> podList = ExcelOperateUtils.importToList(file, ImportExcelV2VO.class);
        log.info("开始转换模型，并调用下载图片");
        String tempPath = System.getProperty("java.io.tmpdir");
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        List<Future<?>> submitList = new ArrayList<>();
        for (int row = 0; row < podList.size(); row++) {
            int finalRow = row;
            ImportExcelV2VO imgDownloadExcelVO = podList.get(finalRow);
            imageOCR(imgDownloadExcelVO, tempPath);
            Future<?> submit = commonThreadPool.submit(() -> {
                try {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            });
            submitList.add(submit);
        }
        for (Future<?> future : submitList) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("多线程报错，", e);
            }
        }
        log.info("开始下载");
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        String fileNameResp = writeLocalPath("ocr_" + fileName, sxssfWorkbook);
        log.info("下载结束，写入本地文件成功 {}", fileNameResp);
    }

    private void imageOCR(ImportExcelV2VO importExcelV2VO, String path) {
        List<String> readyDeliverImg = importExcelV2VO.getReadyDeliverImg();
        for (String img : readyDeliverImg) {
            File file = ImageDownloader.downloadImage(img, path);
            if (Objects.isNull(file)) return;
            InferenceEngine ocr = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
            OcrResult ocrResult = ocr.runOcr(file.toString());
            importExcelV2VO.setOcrContent(ocrResult.getStrRes());
            // 去除末尾的.0
            String dorNumberExe = importExcelV2VO.getDoorNumberExe().replaceAll("\\.0$", "");
            boolean match = ocrResult.getTextBlocks().stream().map(TextBlock::getText).filter(StringUtils::isNotBlank)
                    .anyMatch(text -> text.equalsIgnoreCase(dorNumberExe));
            if (match) {
                importExcelV2VO.setStreetFlag("完全匹配");
                return;
            }
            boolean contains = ocrResult.getTextBlocks().stream().map(TextBlock::getText).filter(StringUtils::isNotBlank)
                    .anyMatch(text -> text.contains(dorNumberExe) || importExcelV2VO.getAddress().contains(text));
            if (contains) {
                importExcelV2VO.setStreetFlag("包含匹配");
                return;
            }
        }
    }


    public static String writeLocalPath(String namePrefix, SXSSFWorkbook workbook) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm-ss");
        String currentTime = dtf.format(LocalDateTime.now());
        String name = namePrefix + currentTime;
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
