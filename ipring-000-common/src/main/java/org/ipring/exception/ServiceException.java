package org.ipring.exception;

import lombok.Getter;
import org.ipring.enums.SubCode;
import org.ipring.enums.subcode.SystemServiceCode;

/**
 * 自定义的业务异常
 *
 * @author lgj
 * @date 8/2/2023
 */
@Getter
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = -2107734356609544839L;
    /**
     * 业务状态码
     */
    private final SubCode subCode;

    /**
     * 自定义提示语
     */
    private final String customizeArguments;


    public ServiceException(SubCode subCode, String customizeArguments) {
        super(customizeArguments);
        this.subCode = subCode;
        this.customizeArguments = customizeArguments;
    }

    public ServiceException(String customizeArguments) {
        this(SystemServiceCode.SystemApi.FAIL, customizeArguments);
    }

    public ServiceException(SubCode subCode) {
        this(subCode, null);
    }
}
