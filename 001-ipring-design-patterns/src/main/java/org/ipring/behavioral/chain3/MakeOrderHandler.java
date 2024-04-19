package org.ipring.behavioral.chain3;


import javax.annotation.PostConstruct;
import java.util.List;

public class MakeOrderHandler extends AbstractValidationHandler<OpenReqInte, Object> {

    private static List<AbstractValidator<? super OpenReqInte, Object>> chain;

    @Override
    public List<AbstractValidator<? super OpenReqInte, Object>> getChain() {
        return chain;
    }

    @Override
    @PostConstruct
    public void buildChain() {
        AbstractValidator.Builder<OpenReqInte, Object> builder = new AbstractValidator.Builder<>();
        chain = builder.addHandler(new PasswordValidator())
                .addHandler(new NotNullValidator())
                .build();
    }
}