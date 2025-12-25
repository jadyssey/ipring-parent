package org.ipring.jacg.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.ipring.excel.ExcelColumn;
import org.ipring.jacg.enums.TargetTypeEnum;
import org.ipring.jacg.process.SimpleCallChainProcessor;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Data
@ApiModel(value = "调用链结果导出模型")
public class CalleeExcelVO {

    @ExcelColumn(0)
    @ApiModelProperty(value = "类型识别")
    private String type;

    @ExcelColumn(1)
    @ApiModelProperty(value = "终点")
    private String target;

    @ExcelColumn(2)
    @ApiModelProperty(value = "起点")
    private String sql;

    @ApiModelProperty(value = "中间链路集合，以“&”分隔")
    @ExcelColumn(3)
    private String chain;

    public static CalleeExcelVO of(String chain) {
        CalleeExcelVO resp = new CalleeExcelVO();
        if (StringUtils.isBlank(chain)) return resp;
        String[] chainStr = chain.split(SimpleCallChainProcessor.SPLIT);
        if (chainStr.length == 1) {
            resp.setSql(chainStr[0]);
            resp.setType(TargetTypeEnum.NO_Target.getDescription());
        } else if (chainStr.length > 1) {
            resp.setTarget(chainStr[0]);
            resp.setType(TargetTypeEnum.checkType(resp.getTarget()));
            resp.setSql(chainStr[1]);
        }
        String chainSource = Arrays.stream(chainStr).skip(2).collect(Collectors.joining(SimpleCallChainProcessor.SPLIT));
        resp.setChain(chainSource);
        return resp;
    }
}
