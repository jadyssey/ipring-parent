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

package org.ipring.behavioral.chain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ipring.behavioral.chain.AbstractChainHandler;
import org.ipring.behavioral.chain.filter.NotNullChainHandler;
import org.ipring.behavioral.chain.filter.ProductChainHandler;
import org.ipring.behavioral.chain.filter.VerificationChainHandler;
import org.ipring.enums.IntEnumType;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单相关责任链 Mark 枚举
 */
@Getter
@AllArgsConstructor
public enum OrderCreateEnum implements IntEnumType {

    // 订单创建校验组
    NOT_NULL(0, "", NotNullChainHandler.class),
    PRODUCT(1, "", ProductChainHandler.class),
    VERIFICATION(2, "", VerificationChainHandler.class),
    ;


    public static final Map<Class<? extends AbstractChainHandler<?>>, OrderCreateEnum> BEAN_ORDER_MAP =
            Arrays.stream(OrderCreateEnum.values()).collect(Collectors.toMap(OrderCreateEnum::getClassType, Function.identity()));


    private final Integer type;
    private final String description;
    private final Class<? extends AbstractChainHandler<?>> classType;
}
