package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.controller.model.TestExcelVO;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.similarity.AddressGroupingNLP;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Api(tags = "测试接口")
@RequestMapping("/test")
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class TestController {

    @PostMapping("/similarity")
    @StlApiOperation(title = "文本相似度算法", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<String> similarity(@RequestParam("file") MultipartFile file) {
        List<TestExcelVO> podList = ExcelOperateUtils.importToList(file, TestExcelVO.class);
        List<String> addressList = podList.stream().map(TestExcelVO::getSource).collect(Collectors.toList());
        List<Set<String>> groupAddrList = AddressGroupingNLP.parallelGroupAddresses(addressList, 0.6);
        int size = groupAddrList.size();
        for (TestExcelVO testExcel : podList) {
            for (int i = 0; i < size; i++) {
                if (groupAddrList.get(i).contains(testExcel.getSource())) {
                    testExcel.setC(String.valueOf(i));
                    break;
                }
            }
        }
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        String fileNameResp = writeLocalPath("DataExtractor", sxssfWorkbook);
        return ReturnFactory.success(fileNameResp);
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
