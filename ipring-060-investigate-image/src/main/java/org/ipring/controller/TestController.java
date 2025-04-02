package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.model.TestExcelVO;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.util.UUIDUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.ipring.controller.PodHandlerController.writeLocalPath;

@Api(tags = "测试接口")
@RequestMapping("/test")
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class TestController {

    @PostMapping("/test")
    @StlApiOperation(title = "空接口", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<String> empty(@RequestParam("file") MultipartFile file) throws IOException {
        List<TestExcelVO> podList = ExcelOperateUtils.importToList(file, TestExcelVO.class);
        for (TestExcelVO testExcel : podList) {
            // 提取 arrivaldate
            String source = testExcel.getSource();
            int arrivalStart = source.indexOf("arrivaldate=") + "arrivaldate=".length();
            int arrivalEnd = source.indexOf(",", arrivalStart);
            String arrivalDate = source.substring(arrivalStart, arrivalEnd);
            testExcel.setTime(arrivalDate);

            // 提取 emailaddress
            int emailStart = source.indexOf("emailaddress=") + "emailaddress=".length();
            int emailEnd = source.indexOf("}", emailStart);
            String email = source.substring(emailStart, emailEnd);
            testExcel.setEmail(email);
            log.info("Arrival Date: {}，Email: {}", arrivalDate, email);
        }
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(podList);
        String fileNameResp = writeLocalPath("DataExtractor", sxssfWorkbook);
        return ReturnFactory.success(fileNameResp);
    }
}
