package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.OrderServiceCode;
import org.ipring.httpclient.impl.Ct4AccountManager;
import org.ipring.model.AccountAddParam;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author lgj
 * @date 2024/5/28
 **/
@Slf4j
@Api(tags = "ct4相关接口")
@RestController
@RequestMapping("/ct4")
@RequiredArgsConstructor
public class Ct4Controller {
    private final Ct4AccountManager ct4AccountManager;

    @PostMapping("/account")
    @StlApiOperation(title = "创建用户", subCodeType = {OrderServiceCode.Order.class}, response = Return.class)
    public Return<String> crateAccount(Integer startUid, Integer endUid) {
        if (Objects.isNull(startUid) || Objects.isNull(endUid)) return ReturnFactory.success();
        for (int j = startUid; j <= endUid; j++) {
            for (int i = 0; i < 20; i++) {
                AccountAddParam param = new AccountAddParam();
                param.setEmail("aaa@apple.com");
                param.setAreaCode("0086");
                param.setDepositCurrency("USD");
                param.setInitFund(new BigDecimal(100000));
                param.setLever(800);
                param.setName("test-" + i);
                param.setPhoneNum("19292919241");
                param.setTradingServerId(1);
                Return<Object> crateAccount = ct4AccountManager.crateAccount(String.valueOf(j), param);
                if (crateAccount.success()) {
                    log.info("成功|创建账号: {}", crateAccount.getBodyMessage());
                } else {
                    log.error("失败|创建账号: {}", crateAccount);
                }
            }
        }
        return ReturnFactory.success();
    }
}
