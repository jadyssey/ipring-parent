package org.ipring.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.ipring.enums.common.ClientTypeInt;
import org.ipring.util.HttpUtils;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author lgj
 * @date 25/11/2022
 **/
@Data
@ApiModel("异常日志模型")
public class LogEntity {
    private String client;
    private String ip;
    private String deviceId;
    private String params;
    private String uri;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String response;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;

    public static LogEntity init() {
        LogEntity entity = new LogEntity();
        entity.setDeviceId(HttpUtils.getDeviceId());
        entity.setClient(ClientTypeInt.getByType(HttpUtils.getClientType()).getDescription());
        ;
        entity.setIp(HttpUtils.getReqIp());
        return entity;
    }
}
