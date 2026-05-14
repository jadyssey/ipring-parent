package org.ipring.jacg.mapper.po;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 类上的注解信息表实体类
 *
 * @author 自动生成
 * @date 2025-12-23
 */
@Data
@ApiModel(value = "JacgClassAnnotationJacg", description = "类上的注解信息表")
public class JacgFormatedSqlVO {
    private String mapperSqlId;
    private String formatedSql;

    public static JacgFormatedSqlVO of(String sql) {
        JacgFormatedSqlVO resp = new JacgFormatedSqlVO();
        resp.setFormatedSql(sql);
        return resp;
    }
}