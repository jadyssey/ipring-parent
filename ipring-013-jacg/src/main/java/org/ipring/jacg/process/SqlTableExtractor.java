package org.ipring.jacg.process;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.ipring.jacg.mapper.po.JacgFormatedSqlVO;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JSqlParser 解析 SQL 提取所有表名工具
 * 支持：子查询、JOIN、WITH子句、UNION、schema、别名等所有复杂场景
 */
@Slf4j
public class SqlTableExtractor {

    public static final List<String> waybillTableList = Arrays.asList("airwaybillno_detail",
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


    // 匹配 MyBatis #{...} 和 ${...}
    private static final Pattern MYBATIS_PLACEHOLDER = Pattern.compile("[#$]\\{[^}]+\\}");
    private static final Pattern IGNORE = Pattern.compile("");

    /**
     * 提取表名（支持MyBatis占位符，不抛解析异常）
     */
    public static List<String> extractTableNamesV2(JacgFormatedSqlVO sqlVO) {
        String sql = sqlVO.getFormatedSql();
        if (sql == null || sql.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 核心：把 #{xxx} / ${xxx} 替换成合法常量，让JSqlParser能正常解析
        String cleanedSql = sqlVO.getFormatedSql().replace("${params.dataScope}", "");
        cleanedSql = MYBATIS_PLACEHOLDER.matcher(cleanedSql).replaceAll("'REPLACED'");

        // 解析SQL
        Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(cleanedSql);
        } catch (JSQLParserException e) {
            log.error("解析报错: {}, {}, exp:", sqlVO.getMapperSqlId(), cleanedSql, e);
            return SqlTableExtractor.waybillTableList.stream().filter(cleanedSql::contains).collect(Collectors.toList());
        }
        if (Objects.isNull(statement)) return Collections.emptyList();
        TablesNamesFinder finder = new TablesNamesFinder();
        // 返回表名列表
        return finder.getTableList(statement);
    }



    /**
     * 提取 SQL 中的所有表名（去重）
     * @param sql 待解析的SQL
     * @return 所有真实表名列表
     * @throws JSQLParserException 解析异常
     */
    public static List<String> extractTableNames(String sql){
        // 步骤1：清理SQL - 去除换行、多余空格，统一格式
        String cleanedSql = sql.replaceAll("\\s+", " ")  // 替换多个空格/换行为单个空格
                .replaceAll("/\\*.*?\\*/", "")  // 去除多行注释
                .replaceAll("--.*", "")  // 去除单行注释
                .trim();
        // 1. 解析SQL生成Statement对象
        Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(cleanedSql);
        } catch (JSQLParserException e) {
            log.error("解析报错: {}, exp:", sql, e);
        }

        // 2. JSqlParser 官方提供的表名查找器（内置遍历所有语法树）
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        
        // 3. 提取所有表名（自动去重）
        return tablesNamesFinder.getTableList(statement);
    }

    // 测试方法
    public static void main(String[] args) throws JSQLParserException {
        // 测试用复杂SQL（嵌套子查询 + JOIN + WITH + UNION）
        /*String sql = """
            WITH temp AS (
                SELECT id FROM user_info WHERE dept_id IN (SELECT id FROM dept_info)
            )
            SELECT u.name, t.id
            FROM temp t
            JOIN user u ON t.id = u.id
            LEFT JOIN order_info o ON u.id = o.user_id
            UNION
            SELECT * FROM product p WHERE p.status = 1
            """;*/
        String sql = "";

        // 提取表名
        List<String> tableNames = extractTableNames(sql);
        
        // 输出结果
        System.out.println("解析出的所有表名：");
        for (String table : tableNames) {
            System.out.println("- " + table);
        }
    }
}