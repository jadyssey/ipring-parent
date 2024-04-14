package org.ipring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.ipring.model.dobj.account.AccountDO;
import org.ipring.model.vo.account.AccountDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/02 10:57
 * @description:
 */
@Mapper
public interface AccountMapper extends BaseMapper<AccountDO> {

    default LambdaQueryChainWrapper<AccountDO> query() {
        return new LambdaQueryChainWrapper<>(this);
    }

    default LambdaUpdateChainWrapper<AccountDO> update() {
        return new LambdaUpdateChainWrapper<>(this);
    }

    /**
     * 根据账户id查询账户详情
     *
     * @param accountId
     * @return
     */
    AccountDetailVO selectAccountDetailByAccountId(Long accountId);

    /**
     * 重置账户密码
     *
     * @param accountId
     * @param pwd
     */
    void resetAccountPwd(@Param("accountId") Long accountId, @Param("pwd") String pwd);

    /**
     * 更新账户交易密码
     *
     * @param accountId
     * @param newTradingPwd
     */
    void updateTradingPwd(@Param("accountId") Long accountId, @Param("newPwd") String newTradingPwd);

    /**
     * 根据账户id批量查询账户列表
     *
     * @param accountIds
     * @return
     */
    List<AccountDetailVO> selectAccountDetailByAccountIds(@Param("accountIds") List<Long> accountIds);

    /**
     * 删除账户
     *
     * @param accountId
     */
    void deleteAccount(Long accountId);

    /**
     * 更新账户观摩密码
     * @param accountId
     * @param newPwd
     */
    void updateViewPwd(@Param("accountId") Long accountId, @Param("newPwd") String newPwd);
}
