package org.ipring.behavioral.chain4;

import lombok.RequiredArgsConstructor;
import org.ipring.behavioral.chain4.base.AbstractValidationHandler;
import org.ipring.behavioral.chain4.base.AbstractValidator;
import org.ipring.behavioral.chain4.base.BaseInte;
import org.ipring.behavioral.chain4.validate.PasswordValidator;
import org.ipring.behavioral.chain4.validate.RoleValidator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author lgj
 * @date 2024/4/17
 */
@Component
@RequiredArgsConstructor
public class BackSystemHandler extends AbstractValidationHandler<BackSystemInte, BaseInte> {

    private static List<AbstractValidator<? super BackSystemInte, BaseInte>> chain;

    @Override
    public List<AbstractValidator<? super BackSystemInte, BaseInte>> getChain() {
        return chain;
    }

    @Override
    @PostConstruct
    public void buildChain() {
        AbstractValidator.Builder<BackSystemInte, BaseInte> builder = new AbstractValidator.Builder<>();
        chain = builder
                .addHandler(new PasswordValidator())
                .addHandler(new RoleValidator())
                .build();
    }
}