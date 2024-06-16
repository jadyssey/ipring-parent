package org.ipring.behavioral.chain4.validate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.behavioral.chain4.base.AbstractValidator;
import org.ipring.behavioral.chain4.base.BaseInte;
import org.ipring.enums.SubCode;
import org.ipring.enums.subcode.SystemServiceCode;
import org.springframework.stereotype.Component;

/**
 * 账号密码校验器
 *
 * @author lgj
 * @date 2024/4/17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleValidator extends AbstractValidator<RoleValidator.RoleInte, BaseInte> {

    @Override
    public SubCode handle0(RoleInte req, BaseInte symbol) {
        System.out.println("req = " + req.getAccountId() + req.getRoleType());
        if (req.getRoleType() == 1) {
            System.out.println("角色校验成功 = " + req.getRoleType());
        }
        return SystemServiceCode.SystemApi.SUCCESS;
    }

    public interface RoleInte {
        Long getAccountId();

        Integer getRoleType();
    }
}