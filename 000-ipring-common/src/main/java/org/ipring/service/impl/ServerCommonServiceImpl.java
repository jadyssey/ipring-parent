package org.ipring.service.impl;

import org.ipring.mapper.ServerMapper;
import org.ipring.model.dobj.server.ServerDO;
import org.ipring.service.ServerCommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/02 11:37
 * @description:
 */
@Service
@RequiredArgsConstructor
public class ServerCommonServiceImpl implements ServerCommonService {

    private final ServerMapper mapper;

    @Override
    public List<ServerDO> getByCollection(Collection<Integer> serverIdList) {

        if (CollectionUtils.isEmpty(serverIdList)) return Collections.emptyList();
        return mapper.selectByIdList(serverIdList);
    }

    @Override
    public ServerDO getById(Integer tradingServerId) {
        return mapper.selectById(tradingServerId);
    }
}
