package org.ipring.mapper;

import org.ipring.model.dobj.server.ServerDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/02 11:37
 * @description:
 */
@Mapper
public interface ServerMapper {

    List<ServerDO> selectByIdList(@Param("idList") Collection<Integer> idList);

    ServerDO selectById(@Param("id") Integer tradingServerId);
}
