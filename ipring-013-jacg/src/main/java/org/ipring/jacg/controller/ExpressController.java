package org.ipring.jacg.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.ipring.jacg.mapper.ClassAnnotationMapper;
import org.ipring.jacg.mapper.po.JacgClassAnnotationPO;
import org.ipring.jacg.model.ApiResult;
import org.ipring.jacg.model.ExpressStatus;
import org.ipring.jacg.process.SimpleCallChainProcessor;
import org.ipring.jacg.process.SqlTableExtractor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/express")
public class ExpressController {
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
     * @param sqlStr
     * @return
     */
    @PostMapping("/extract/tagble")
    public HashMap<String, String> extractTable(@RequestBody String sqlStr) {
        List<String> waybillTableList = Arrays.asList("airwaybillno_detail", "airwaybillno_info", "airwaybillno_operate_log", "airwaybillno_sync", "mis_waybill_error_address", "mis_waybill_error_address_feedback", "mis_waybill_error_address_log", "mis_waybill_error_address_send_log", "mis_waybill_expand", "mis_waybill_goods", "mis_waybill_hub_change_log", "mis_waybill_info", "mis_waybill_init_metrics", "mis_waybill_item", "mis_waybill_label", "mis_waybill_label_print_record", "mis_waybill_lifecycle_history", "mis_waybill_operate_historial", "mis_waybill_return", "mis_waybill_return_structured_address", "mis_waybill_sorting_no_ref_mapping", "mis_waybill_structured_address", "receive_waybill_info", "route_location_log", "third_party_history", "third_party_labels", "third_party_waybills", "waybill_check_against_items", "third_barn_order_info");

        List<String> sqlList = new ArrayList<>(Arrays.asList(sqlStr.split("@")));
        Set<String> allTableList = new HashSet<>();
        HashMap<String, String> tableMap = new HashMap<>();
        for (String sql : sqlList) {
            String[] split = sql.split("&");
            if (split.length != 2) {
                continue;
            }
            List<String> tableNameList = SqlTableExtractor.extractTableNamesV2(split[1]);
            allTableList.addAll(tableNameList);
            for (String table : tableNameList) {
                if (waybillTableList.contains(table.toLowerCase())) {
                    tableMap.put(split[0], String.join(",", tableNameList));
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