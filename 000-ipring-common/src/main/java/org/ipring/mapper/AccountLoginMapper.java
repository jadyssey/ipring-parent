package org.ipring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ipring.model.dobj.account.AccountLoginDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: Rainful
 * @date: 2024/04/02 14:47
 * @description:
 */
@Mapper
public interface AccountLoginMapper extends BaseMapper<AccountLoginDO> {
}
