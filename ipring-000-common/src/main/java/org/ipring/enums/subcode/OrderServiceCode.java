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
@StlServiceCode(code = ServiceSubCodeStr.ORDER)
public interface OrderServiceCode {
    @Getter
    @AllArgsConstructor
    enum Order implements SubCode {
        // 下单接口
        WAIT_MIN("0101", "操作频繁，请稍后再试", "common.frequently"),
        PARAM_ERROR("0102", "请求参数错误", "common.illegal"),
        TIME_LIMIT("0105", "不在品种交易时间内", "order.limit.time"),
        HOLIDAY_LIMIT("0106", "节假日限制", "order.limit.holiday"),
        SYMBOL_AUTH_LIMIT("0107", "品种权限限制", "order.limit.symbol.auth"),
        TRADE_AUTH_LIMIT("0108", "交易权限限制", "order.limit.trade.auth"),
        UP_OR_DOWN_LIMIT("0109", "涨跌停限制", "order.limit.up.or.down"),
        SUS_OR_DEL_LIMIT("0110", "停牌退市限制", "order.limit.sus.or.del"),
        AVAILABLE_FUNDS_LIMIT("0111", "可用资金限制", "order.limit.available.funds"),
        PASSWORD_LIMIT("0112", "账户密码限制", "order.limit.password"),
        SLIPPAGE_ERROR("0113", "滑点误差校验", "order.slippage.error"),
        MARGIN_ERROR("0114", "保证金校验", "order.margin.error"),
        TICKET_ILLEGAL("0115", "交易手数非法", "order.ticket.error"),
        STOPS_LEVEL("0116", "停损级别限制", "order.sybol.stop.level.error"),
        EXPIRATION_TYPE_ERROR("0117", "过期时间配置非法", "common.illegal"),


        ORDER_CREAT("0120", "订单创建失败", "order.create.error"),
        SYBOL_ERROR("0121", "品种报价获取失败", "order.sybol.error"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }

    @Getter
    @AllArgsConstructor
    enum QueryOrder implements SubCode {
        // 查询接口
        WAIT_MIN("0201", "操作频繁，请稍后再试", "common.frequently"),
        PARAM_ERROR("0203", "请求参数错误", "common.illegal"),
        SYBOL_ERROR("0204", "品种报价获取失败", "order.sybol.error"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }

    @Getter
    @AllArgsConstructor
    enum CloseOrder implements SubCode {
        // 平仓接口
        PARAM_ERROR("0301", "请求参数错误", "common.illegal"),
        CLOSE_FORBIDDEN("0302", "品种不可平仓", "order.close.forbidden"),
        PASSWORD_LIMIT("0303", "账户密码限制", "order.limit.password"),
        TIME_LIMIT("0304", "不在品种交易时间内", "order.limit.time"),
        HOLIDAY_LIMIT("0305", "节假日限制", "order.limit.holiday"),
        UP_OR_DOWN_LIMIT("0306", "涨跌停限制", "order.limit.up.or.down"),
        SUS_OR_DEL_LIMIT("0307", "停牌退市限制", "order.limit.sus.or.del"),
        CLOSE_SYMBOL_NO_ASK_PRICE("0350", "品种实时报价未查询到", "order.symbol.no_ask_price"),
        CLOSE_UPDATE_FAILURE("0390", "部分平仓更新行数为0", "common.frequently"),
        CLOSE_INSERT_FAILURE("0391", "部分平仓新增行数为0", "common.frequently"),
        CLOSE_ALL_FAILURE("0399", "全部平仓更新行数为0", "common.frequently"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }

    @Getter
    @AllArgsConstructor
    enum UpdateOrder implements SubCode {
        // 修改订单接口
        WAIT_MIN("0401", "操作频繁，请稍后再试", "common.frequently"),
        PARAM_ERROR("0403", "请求参数错误", "common.illegal"),
        UPDATE_FAIL("0405", "订单更新失败", "order.update.fail"),
        TRADE_AUTH_LIMIT("0408", "交易权限限制", "order.limit.trade.auth"),
        ORDER_STATUS_LIMIT("0409", "当前订单状态无法修改", "order.limit.status.auth"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }

    @Getter
    @AllArgsConstructor
    enum DelOrder implements SubCode {
        // 修改订单接口
        DEL_FAIL("0501", "订单删除失败", "order.del.fail"),
        ORDER_NOT_FOUND("0502", "请求参数错误", "common.illegal"),
        ORDER_FORBID_DEL("0503", "只允许删除挂单", "order.del.fail"),
        DEL_TIME_LIMIT("0504", "集合竞价时段不允许删除", "order.del.fail"),
        PARAM_ERROR("0505", "请求参数错误", "common.illegal"),
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }

    @Getter
    @AllArgsConstructor
    enum MarginCalc implements SubCode {
        // 平仓接口
        SYMBOL_NOT_EXIST("0601", "计算保证金品种不存在", "common.illegal")
        ;

        private final String code;
        private final String desc;
        private final String i18nKey;
    }
}
