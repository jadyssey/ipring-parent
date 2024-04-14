package org.ipring.service;

import org.ipring.model.dobj.server.ServerDO;

import java.util.Collection;
import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/02 11:35
 * @description:
 */
public interface ServerCommonService {

    List<ServerDO> getByCollection(Collection<Integer> serverIdList);

    ServerDO getById(Integer tradingServerId);
}
