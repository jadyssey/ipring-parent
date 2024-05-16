package org.ipring.client;

import org.ipring.client.dto.OrderAddDTO;
import org.ipring.client.response.ct4.ModifyOrderVO;
import org.ipring.model.common.Return;

public interface Ct4ServiceManager {

    Return<ModifyOrderVO> makeOrder(String ct4Token, OrderAddDTO orderAddDTO);
}
