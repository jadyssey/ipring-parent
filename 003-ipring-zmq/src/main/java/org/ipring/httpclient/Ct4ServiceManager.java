package org.ipring.httpclient;

import org.ipring.model.httpclient.dto.OrderAddDTO;
import org.ipring.model.httpclient.response.ct4.ModifyOrderVO;
import org.ipring.model.common.Return;

public interface Ct4ServiceManager {

    Return<ModifyOrderVO> makeOrder(String ct4Token, OrderAddDTO orderAddDTO);
}
