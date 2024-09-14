package org.ipring.httpclient;

import org.ipring.model.common.Return;
import org.ipring.model.httpclient.dto.OrderAddDTO;
import org.ipring.model.httpclient.response.ct4.ModifyOrderVO;

public interface Ct4ServiceManager {

    Return<ModifyOrderVO> makeOrder(String ct4Token, OrderAddDTO orderAddDTO);
}
