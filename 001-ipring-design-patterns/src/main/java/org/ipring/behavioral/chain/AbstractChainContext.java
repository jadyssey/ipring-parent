/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ipring.behavioral.chain;

import com.google.common.collect.Maps;
import org.ipring.behavioral.chain.enums.OrderUpdateEnum;
import org.ipring.behavioral.chain.enums.OrderCreateEnum;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 抽象责任链上下文
 *
 * @author chen.ma
 * @github <a href="https://github.com/opengoofy" />
 * @公众号 马丁玩编程，关注回复：资料，领取后端技术专家成长手册
 */
@Component
public final class AbstractChainContext<T> implements CommandLineRunner {

    private final Map<Class<?>, AbstractChainHandler<T>[]> abstractChainHandlerContainer = Maps.newHashMap();

    /**
     * 责任链组件执行
     *
     * @param groupEnum    责任链组件标识
     * @param requestParam 请求参数
     */
    public void handler(Class<?> groupEnum, T requestParam) {
        AbstractChainHandler<T>[] abstractChainHandlers = abstractChainHandlerContainer.get(groupEnum);
        if (abstractChainHandlers == null || abstractChainHandlers.length == 0) {
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", groupEnum));
        }
        for (AbstractChainHandler<T> abstractChainHandler : abstractChainHandlers) {
            abstractChainHandler.handler(requestParam);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, AbstractChainHandler> chainFilterMap = ApplicationContextHolder.getBeansOfType(AbstractChainHandler.class);
        chainFilterMap.forEach((beanName, bean) -> {
            Optional.ofNullable(OrderCreateEnum.BEAN_ORDER_MAP.get(bean.getClass())).ifPresent(orderCreate -> {
                AbstractChainHandler<T>[] handlers = abstractChainHandlerContainer.computeIfAbsent(OrderCreateEnum.class, k -> new AbstractChainHandler[OrderCreateEnum.BEAN_ORDER_MAP.size()]);
                handlers[orderCreate.getType()] = bean;
            });
            Optional.ofNullable(OrderUpdateEnum.BEAN_ORDER_MAP.get(bean.getClass())).ifPresent(orderUpdate -> {
                AbstractChainHandler<T>[] handlers = abstractChainHandlerContainer.computeIfAbsent(OrderUpdateEnum.class, k -> new AbstractChainHandler[OrderUpdateEnum.BEAN_ORDER_MAP.size()]);
                handlers[orderUpdate.getType()] = bean;
            });
        });
    }
}
