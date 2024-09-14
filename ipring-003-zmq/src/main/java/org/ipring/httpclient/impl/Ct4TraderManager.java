package org.ipring.httpclient.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.httpclient.ICt4TraderManager;
import org.ipring.model.common.Return;
import org.ipring.model.httpclient.dto.OrderAddDTO;
import org.ipring.model.httpclient.response.ct4.ModifyOrderVO;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Ct4TraderManager implements ICt4TraderManager {

    private final ICt4TraderManager iCt4Manager;

    @Override
    public Return<ModifyOrderVO> makeOrder(String ct4Token, OrderAddDTO orderAddDTO) {
        Return<ModifyOrderVO> modifyOrderVOReturn = iCt4Manager.makeOrder(ct4Token, orderAddDTO);
        if (modifyOrderVOReturn.success()) {
            log.info("success make order : {}", JsonUtils.toJson(modifyOrderVOReturn));
        } else {
            log.info("fail make order : {}", JsonUtils.toJson(modifyOrderVOReturn));
        }
        return modifyOrderVOReturn;
    }
}
