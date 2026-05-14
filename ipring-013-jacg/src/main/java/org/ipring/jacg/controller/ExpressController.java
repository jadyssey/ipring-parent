package org.ipring.jacg.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.ipring.jacg.mapper.ClassAnnotationMapper;
import org.ipring.jacg.mapper.po.JacgFormatedSqlVO;
import org.ipring.jacg.model.ApiResult;
import org.ipring.jacg.model.ExpressStatus;
import org.ipring.jacg.process.SqlTableExtractor;
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
    public HashMap<String, String> extractTable() {

        List<String> waybillTableList = Arrays.asList("airwaybillno_detail",
                "airwaybillno_info",
                "airwaybillno_operate_log",
                "airwaybillno_sync",
                "mis_box_scan_record",
                "mis_waybill_bagging_log",
                "mis_waybill_error_address",
                "mis_waybill_error_address_feedback",
                "mis_waybill_error_address_log",
                "mis_waybill_error_address_send_log",
                "mis_waybill_expand",
                "mis_waybill_goods",
                "mis_waybill_hub_change_log",
                "mis_waybill_info",
                "mis_waybill_init_metrics",
                "mis_waybill_item",
                "mis_waybill_label",
                "mis_waybill_label_print_record",
                "mis_waybill_lifecycle_history",
                "mis_waybill_operate_historial",
                "mis_waybill_return",
                "mis_waybill_return_structured_address",
                "mis_waybill_sorting_no_ref_mapping",
                "mis_waybill_structured_address",
                "receive_waybill_info",
                "route_location_log",
                "third_barn_order_info",
                "third_party_history",
                "third_party_labels",
                "third_party_waybills",
                "waybill_check_against_items");

        List<JacgFormatedSqlVO> formattedSqlList;
        formattedSqlList = classAnnotationMapper.selectAllFormatedSql();

        Set<String> allTableList = new HashSet<>();
        HashMap<String, String> tableMap = new HashMap<>();
        for (JacgFormatedSqlVO sql : formattedSqlList) {
            if (!waybillTableList.stream().anyMatch(table -> sql.getFormatedSql().contains(table))) {
                // 这条SQL没有需要梳理的表
                continue;
            }
            List<String> tableNameList = SqlTableExtractor.extractTableNamesV2(sql);
            allTableList.addAll(tableNameList);
            for (String table : tableNameList) {
                if (waybillTableList.contains(table.toLowerCase())) {
                    tableMap.put(sql.getMapperSqlId(), String.join(",", tableNameList));
                    break;
                }
            }
        }
        allTableList.forEach(System.out::println);
        System.out.println(" =================================================");
        tableMap.keySet().forEach(System.out::println);
        return tableMap;
    }



}