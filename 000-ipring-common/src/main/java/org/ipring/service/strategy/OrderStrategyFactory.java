package org.ipring.service.strategy;

import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单策略模式工厂类
 *
 * @author lgj
 * @date 2024/4/3
 **/
@Component
public class OrderStrategyFactory {

    private final EnumMap<OrderTypeEnum, MakeOrderStrategy> strategyMap;

    @Autowired
    public OrderStrategyFactory(ListableBeanFactory beanFactory) {
        strategyMap = beanFactory.getBeansOfType(MakeOrderStrategy.class).values().stream().collect(Collectors.toMap(
                e -> Objects.requireNonNull(AnnotationUtils.findAnnotation(e.getClass(), OrderType.class)).value(),
                Function.identity(), (e1, e2) -> e1, () -> new EnumMap<>(OrderTypeEnum.class)));
    }

    public MakeOrderStrategy getOrderStrategy(Integer orderType) {
        return Optional.ofNullable(orderType).map(OrderTypeEnum.ALL_ENUM_MAP::get).map(strategyMap::get)
                .orElseThrow(() -> new ServiceException(SystemServiceCode.SystemApi.PARAM_ERROR));
    }
}