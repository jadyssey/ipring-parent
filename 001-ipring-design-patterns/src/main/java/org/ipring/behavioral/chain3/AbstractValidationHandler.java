package org.ipring.behavioral.chain3;


import java.util.List;

public abstract class AbstractValidationHandler<T, E> implements ValidationHandler<T, E> {

    protected abstract void buildChain();

    protected abstract List<AbstractValidator<? super T, Object>> getChain();

    @Override
    public void handleReq(T req, E entry) {
    }
}