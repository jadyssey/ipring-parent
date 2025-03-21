package org.ipring.controller;

import cn.hutool.extra.qrcode.QrCodeUtil;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.gateway.DeliveryGateway;
import org.ipring.model.ImgDecodeResp;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.model.common.ZtReturn;
import org.ipring.model.delivery.AmazonBatchFileVO;
import org.ipring.model.delivery.ImgDownloadExcelVO;
import org.ipring.model.delivery.ReasonDownloadExcelVO;
import org.ipring.model.enums.NonComplianceReason;
import org.ipring.util.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author liuguangjin
 * @date 2025/2/11
 */
@Api(tags = "pod相关处理")
@RequestMapping("/pod")
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class PodHandlerController {
    private final DeliveryGateway deliveryGateway;
    private final ThreadPoolTaskExecutor commonThreadPool;

    @PostMapping("/img-qr")
    @StlApiOperation(title = "pod图片二维码识别", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ImgDecodeResp> imageDecode(@RequestParam String imageUrl) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new URL(imageUrl));

        // 2. 获取工具实例（自动初始化 OpenCV 和模型）
        WeChatQRCodeTool tool = WeChatQRCodeTool.getInstance();
        // 3. 解码二维码
        String weChatResult = tool.decode(bufferedImage);
        // 对比谷歌二维码识别
        String googleZxing = QrCodeUtil.decode(bufferedImage);
        String customDecode = CustomDecodeUtil.decode(bufferedImage);
        ImgDecodeResp resp = new ImgDecodeResp();
        resp.setWeChatQRCodeTool(weChatResult);
        resp.setGoogleZxing(googleZxing);
        resp.setCustomDecodeUtil(customDecode);

//        String decode = CustomFileDecodeUtil.decode("D:\\img");
//        resp.setCustomDecodeUtilV2(decode);
        return ReturnFactory.success(resp);
    }

    @PostMapping("/img-process")
    @StlApiOperation(title = "pod图片二维码处理", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<?> imageProcess(@RequestParam String imageUrl, @RequestParam Integer tool) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new URL(imageUrl));
        int cvtype = CvType.CV_8UC3;
        if (bufferedImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            cvtype = CvType.CV_8UC1;
        }
        OpenCVLoader.loadOpenCV();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Mat image = WeChatQRCodeTool.bufImg2Mat(bufferedImage, bufferedImage.getType(), cvtype);
        Mat qrCodeAndCut = ImageHandlerUtil.findQRCodeAndCut(image);
        if (qrCodeAndCut == null) return ReturnFactory.error();

        Mat mat = ImageHandlerUtil.processAndThresholdImage(qrCodeAndCut);
        ImageHandlerUtil.createCLAHE(mat);
        return ReturnFactory.success();
    }


    @PostMapping("/url-convert")
    @StlApiOperation(title = "pod图片地址转换", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void importExcelAndExport(@RequestParam("file") MultipartFile file, HttpServletResponse response, @RequestParam String nation, @RequestParam String fileName) {
        List<ImgDownloadExcelVO> podList = ExcelOperateUtils.importToList(file, ImgDownloadExcelVO.class);
        String secretKey = HttpUtils.getHeader("Authorization");
        log.info("开始转换模型，并调用下载图片");
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        List<Future<?>> submitList = new ArrayList<>();
        for (int row = 0; row < podList.size(); row++) {
            int finalRow = row;
            Future<?> submit = commonThreadPool.submit(() -> {
                try {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                    imageConvert(podList, secretKey, finalRow, nation);
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            });
            submitList.add(submit);
        }
        try {
            for (Future<?> future : submitList) {
                future.get();
            }
        } catch (Exception e) {
            log.error("多线程报错，", e);
        }
        log.info("开始下载");
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        String fileNameResp = writeLocalPath("download_img_" + fileName, sxssfWorkbook);
        log.info("下载结束，写入本地文件成功 {}", fileName);
    }

    private void imageConvert(List<ImgDownloadExcelVO> podList, String auth, int row, String nation) {
        ImgDownloadExcelVO imgDownloadExcelVO = podList.get(row);
        List<String> imgList = Arrays.asList(imgDownloadExcelVO.getImages().split(","));
        AmazonBatchFileVO amazonBatchFileVO = new AmazonBatchFileVO();
        amazonBatchFileVO.setFileKeyList(imgList);
        amazonBatchFileVO.setAuthorization(auth);
        ZtReturn<List<String>> listReturn = new ZtReturn<>();
        if (nation.equals("us")) {
            listReturn = deliveryGateway.usBatchDownloadImg(amazonBatchFileVO);
        } else if (nation.equals("fr")) {
            listReturn = deliveryGateway.frBatchDownloadImg(amazonBatchFileVO);
        }
        if (listReturn.success() && listReturn.hashData()) {
            for (int i = 0; i < listReturn.getData().size(); i++) {
                if (i == 0) {
                    imgDownloadExcelVO.setImage1(listReturn.getData().get(i));
                } else if (i == 1) {
                    imgDownloadExcelVO.setImage2(listReturn.getData().get(i));
                } else if (i == 2) {
                    imgDownloadExcelVO.setImage3(listReturn.getData().get(i));
                } else if (i == 3) {
                    imgDownloadExcelVO.setImage4(listReturn.getData().get(i));
                } else if (i == 4) {
                    imgDownloadExcelVO.setImage5(listReturn.getData().get(i));
                } else {
                    log.error("第i={}行， 图片丢失保存，总图片数量为：{}, data={}", row, listReturn.getData().size(), JsonUtils.toJson(listReturn.getData()));
                }
            }
        } else {
            log.error("第 {} 行，图片下载失败，{}", row, JsonUtils.toJson(listReturn));
        }
    }

    @PostMapping("/reason-convert")
    @StlApiOperation(title = "pod不合规原因转换", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void reasonConvert(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ReasonDownloadExcelVO> podList = ExcelOperateUtils.importToList(file, ReasonDownloadExcelVO.class);

        podList = podList.stream().peek(reason -> {
            String reasonConvert = Arrays.stream(reason.getReason().split(":")).map(Double::valueOf)
                    .map(Double::intValue)
                    .map(NonComplianceReason::getByCode).map(NonComplianceReason::getDescription)
                    .collect(Collectors.joining(","));
            reason.setReasonConvert(reasonConvert);
        }).collect(Collectors.toList());
        log.info("开始下载");
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        String fileName = writeLocalPath("download_reason", sxssfWorkbook);
        log.info("下载结束，写入本地文件成功 {}", fileName);
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
}

