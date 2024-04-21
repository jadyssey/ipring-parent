package org.ipring.behavioral.chain2.group;

import org.ipring.behavioral.chain2.AbstractValidator;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractValidationHandler<T> implements ValidationHandler<T> {

    protected abstract void buildChain();

    protected abstract AbstractValidator<T> getChain();

    @Override
    public void handleReq(T req) {
        getChain().handle(req);
    }
}