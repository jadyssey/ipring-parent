package org.ipring.service;

import org.ipring.model.entity.account.AccountCacheEntity;

/**
 * @author: Rainful
 * @date: 2024/04/11 20:09
 * @description:
 */
public interface AccountCommonService {

    AccountCacheEntity getByAccountId(Long accountId);
}
