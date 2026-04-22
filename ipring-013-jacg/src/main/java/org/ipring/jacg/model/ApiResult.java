package org.ipring.jacg.model;

import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;

@Data
public class ApiResult{
    private Integer code;
    private String msg;
    private PageData data;

    @Data
    public class PageData {
        private List<ExpressStatus> records;
        private Integer total;
        private Integer size;
        private Integer current;
        private Integer pages;
    }
}