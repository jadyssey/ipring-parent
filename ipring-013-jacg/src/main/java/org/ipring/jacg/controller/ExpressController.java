package org.ipring.jacg.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.ehcache.core.util.CollectionUtil;
import org.ipring.jacg.mapper.ClassAnnotationMapper;
import org.ipring.jacg.mapper.po.JacgFormatedSqlVO;
import org.ipring.jacg.model.ApiResult;
import org.ipring.jacg.model.ExpressStatus;
import org.ipring.jacg.process.SqlTableExtractor;
import org.ipring.util.JsonUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/express")
public class ExpressController {
    private final ClassAnnotationMapper classAnnotationMapper;

    /**
     * 接收原始JSON，解析并返回 itemCode + itemValue 列表
     */
    @PostMapping("/parse-status")
    public List<String> parseExpressStatus(@RequestBody ApiResult json) {
        List<String> resp = json.getData().getRecords().stream().sorted(Comparator.comparing(ExpressStatus::getItemCode))
                .map(es -> es.getItemCode() + "#" + es.getItemValue()).collect(Collectors.toList());
        System.out.println(" ========================================= ");
        resp.forEach(System.out::println);
        return resp;
    }

    /**
     * apifox导出接口json格式提取uri
     * @param openApiJson
     * @return
     */
    @PostMapping("/extract/uri")
    public List<String> extractUri(@RequestBody String openApiJson) {
        List<String> uriList = new ArrayList<>();

        // 解析JSON
        JSONObject jsonObject = JSONObject.parseObject(openApiJson);
        // 获取所有path
        JSONObject paths = jsonObject.getJSONObject("paths");

        if (paths != null) {
            Set<String> uris = paths.keySet();
            uriList.addAll(uris);
        }
        uriList.forEach(System.out::println);
        return uriList;
    }


    /**
     * select CONCAT("@", mapper_class_name,":",sql_id,"&",formated_sql) from jacg_mybatis_ms_formated_sql_jacg
     * 查出来的数据丢进来分析
     *
     * @return
     */
    @PostMapping("/extract/table")
    public List<String> extractTable(@RequestBody List<String> tableList) {
        if (CollectionUtils.isEmpty(tableList)) {
            tableList = SqlTableExtractor.waybillTableList;
        }
        List<JacgFormatedSqlVO> formattedSqlList;
        formattedSqlList = classAnnotationMapper.selectAllFormatedSql();

        Set<String> allTableList = new HashSet<>();
        HashMap<String, String> tableMap = new HashMap<>();
        for (JacgFormatedSqlVO sql : formattedSqlList) {
            if (tableList.stream().noneMatch(table -> sql.getFormatedSql().toLowerCase().contains(table))) {
                // 这条SQL没有需要梳理的表
                continue;
            }
            List<String> tableNameList = SqlTableExtractor.extractTableNamesV2(sql, tableList);
            allTableList.addAll(tableNameList);
            for (String table : tableNameList) {
                if (tableList.contains(table.toLowerCase())) {
                    tableMap.put(sql.getMapperSqlId(), String.join(",", tableNameList));
                    break;
                }
            }
        }

        System.out.println(" =================================================");
        tableMap.forEach((key,value) -> System.out.println("key = " + key + ", value = " + value));
        return new ArrayList<>(tableMap.keySet());
    }



}