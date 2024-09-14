package org.ipring.behavioral.chain2.group;

import org.ipring.behavioral.chain2.AbstractValidator;
import org.ipring.behavioral.chain2.FormatValidator;
import org.ipring.behavioral.chain2.NullValidator;
import org.ipring.behavioral.chain2.TypeValidator;
import org.springframework.stereotype.Component;

@Component
public class OrderCreateChain extends AbstractValidationHandler<String> {

    private AbstractValidator<String> chain;

    public OrderCreateChain() {
        buildChain();
    }

    @Override
    public AbstractValidator<String> getChain() {
        return chain;
    }

    @Override
    public void buildChain() {
        AbstractValidator.Builder<String> builder = new AbstractValidator.Builder<>();
        chain = builder.addHandler(new FormatValidator())
                .addHandler(new TypeValidator())
                .addHandler(new NullValidator())
                .build();
    }
}