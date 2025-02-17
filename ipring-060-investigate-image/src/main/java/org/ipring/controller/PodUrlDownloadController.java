package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.gateway.DeliveryGateway;
import org.ipring.model.common.Return;
import org.ipring.model.common.ZtReturn;
import org.ipring.model.delivery.AmazonBatchFileVO;
import org.ipring.model.delivery.ImgDownloadExcelVO;
import org.ipring.util.JsonUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author liuguangjin
 * @date 2025/2/11
 */
@Api(tags = "pod 地址下载")
@RequestMapping("/pod")
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class PodUrlDownloadController {
    private final DeliveryGateway deliveryGateway;

    @PostMapping("/url-convert")
    @StlApiOperation(title = "导入数据批量转换", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void importExcelAndExport(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ImgDownloadExcelVO> podList = ExcelOperateUtils.importToList(file, ImgDownloadExcelVO.class);

        log.info("开始转换模型，并逐条调用下载图片");
        int row = 1;
        for (ImgDownloadExcelVO imgDownloadExcelVO : podList) {
            List<String> imgList = Arrays.asList(imgDownloadExcelVO.getImages().split(","));
            AmazonBatchFileVO amazonBatchFileVO = new AmazonBatchFileVO();
            amazonBatchFileVO.setFileKeyList(imgList);
            ZtReturn<List<String>> listReturn = deliveryGateway.batchDownloadImg(amazonBatchFileVO);
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
            row++;
        }
        log.info("开始下载");
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        String fileName = writeLocalPath("download_img", sxssfWorkbook);
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
