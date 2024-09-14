package org.ipring.behavioral.chain4;

import org.ipring.behavioral.chain4.base.BaseInte;
import org.ipring.behavioral.chain4.validate.PasswordValidator;
import org.ipring.behavioral.chain4.validate.RoleValidator;

/**
 * @author lgj
 * @date 2024/6/4
 **/
public interface BackSystemInte extends BaseInte, PasswordValidator.PasswordInte, RoleValidator.RoleInte {
}
