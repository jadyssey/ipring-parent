package org.ipring.enums.subcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ipring.anno.StlServiceCode;
import org.ipring.enums.SubCode;
import org.ipring.enums.common.ServiceSubCodeStr;

/**
 * @author lgj
 * @date 9/2/2023
 **/
@StlServiceCode(code = ServiceSubCodeStr.SYSTEM)
public interface SystemServiceCode {
    @Getter
    @AllArgsConstructor
    enum SystemApi implements SubCode {
        // 系统保留
        SUCCESS("0000", "ok", ""),
        ENCRYPTED_SUCCESS("EC", "ok", ""),
        WAIT_MIN("0001", "操作频繁，请稍后再试", "common.frequently"),
        LOCK_FREQUENTLY_MIN("0002", "操作频繁，请稍后再试", "common.frequently"),
        FAIL("0003", "服务器繁忙,请重试", "common.server-busy"),
        PARAM_ERROR("0004", "请求参数错误", "common.illegal"),
        DECRYPT_FAILURE("0006", "decrypt error", ""),
        LOGIN_EXPIRE("0007", "登录失效，请重新登录", "common.login.expire"),
        CLIENT_ERROR("0022", "client error", ""),
        LANG_ERROR("0023", "language error", ""),
        SNOW_FLAKE("0024", "雪花算法异常", "common.server-busy"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }

    @Getter
    @AllArgsConstructor
    enum Email implements SubCode {
        // 邮件相关
        DOMAIN("0101", "邮箱域名不合法，请更换邮箱后重试", "common.email.domain.error"),
        FORMAT("0102", "邮箱格式错误，请重新输入", "common.email.format.error"),
        FREQUENTLY("0103", "操作频繁，请稍后再试", "common.email.frequently.error"),
        EXPIRE("0104", "当前验证码已失效，请重新获取", "common.email.expire.error"),
        WRONG("0105", "验证码错误", "common.email.wrong.error"),
        LIMIT_DEVICE("0106", "感谢您对本活动的支持，当前设备已达今日投票上限。", "common.email.frequently.device"),
        LIMIT_IP("0107", "感谢您对本活动的支持，当前网络投票人数过多，请更换网络或稍后再试。", "common.email.frequently.ip"),
        LIMIT_EMAIL("0108", "感谢您对本活动的支持，当前邮箱已达今日投票上限。", "common.email.frequently.email"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }

    @Getter
    @AllArgsConstructor
    enum TradeCalcula implements SubCode {
        // 交易计算相关异常
        PARAM_ERROR("0201", "请求参数错误", "common.illegal"),
        SYMBOL_PRICE("0205", "汇率转换失败", "common.trade.error"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }
}
