package org.ipring.client.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.client.Ct4ServiceManager;
import org.ipring.client.ICt4Service;
import org.ipring.client.dto.OrderAddDTO;
import org.ipring.client.response.ct4.ModifyOrderVO;
import org.ipring.model.common.Return;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Ct4ServiceManagerImpl implements Ct4ServiceManager {

    private final ICt4Service ct4Service;

    @Override
    public Return<ModifyOrderVO> makeOrder(String ct4Token, OrderAddDTO orderAddDTO) {
        Return<ModifyOrderVO> modifyOrderVOReturn = ct4Service.makeOrder(ct4Token, orderAddDTO);
        if (modifyOrderVOReturn.success()) {
            log.info("success make order : {}", JsonUtils.toJson(modifyOrderVOReturn));
        } else {
            log.info("fail make order : {}", JsonUtils.toJson(modifyOrderVOReturn));
        }
        return modifyOrderVOReturn;
    }
}
