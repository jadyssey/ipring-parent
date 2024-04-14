package org.ipring.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.ipring.constant.AccountConstant;
import org.ipring.enums.common.BoolTypeInt;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.mapper.AccountMapper;
import org.ipring.model.dobj.account.AccountDO;
import org.ipring.model.entity.account.AccountCacheEntity;
import org.ipring.service.AccountCommonService;
import org.ipring.util.RedisKeyUtil;
import org.ipring.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Rainful
 * @date: 2024/04/11 20:10
 * @description:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountCommonServiceImpl implements AccountCommonService {

    private final RedisUtil redisUtil;
    private final AccountMapper accountMapper;

    @Override
    public AccountCacheEntity getByAccountId(Long accountId) {

        final String redisKey = RedisKeyUtil.accountKey(accountId);
        final AccountCacheEntity data = (AccountCacheEntity) redisUtil.hGet(redisKey, AccountConstant.accountStoreKey); // 将每个账号-1位用户id设置为账号本身的属性
        if (data != null) return data;

        final AccountDO accountDO = accountMapper.selectOne(new QueryWrapper<AccountDO>().lambda().eq(AccountDO::getId, accountId));
        if (null == accountDO) {
            log.error("查询账户信息 accountId:{}未查询到任何信息", accountId);
            throw new ServiceException(SystemServiceCode.SystemApi.PARAM_ERROR);
        }

        if (BoolTypeInt.isTrue(accountDO.getDel())) {
            log.error("查询账户信息 accountId:{}已被删除", accountId);
            throw new ServiceException(SystemServiceCode.SystemApi.FAIL);
        }
        final AccountCacheEntity entity = AccountCacheEntity.of(accountDO);
        redisUtil.hAdd(redisKey, AccountConstant.accountStoreKey, entity);
        return entity;
    }
}
