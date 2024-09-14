package org.ipring.behavioral.chain4.base;


import org.ipring.enums.SubCode;
import org.ipring.enums.subcode.SystemServiceCode;

import java.util.List;


/**
 * @author lgj
 * @date 2024/4/17
 */
public abstract class AbstractValidationHandler<T, E> implements ValidationHandler<T, E> {

    protected abstract void buildChain();

    protected abstract List<AbstractValidator<? super T, E>> getChain();

    @Override
    public SubCode handleReq(T req, E entry) {
        List<AbstractValidator<? super T, E>> chain = getChain();
        for (AbstractValidator<? super T, E> handle : chain) {
            SubCode subcode = handle.handle(req, entry);
            if (!subcode.success()) return subcode;
        }
        return SystemServiceCode.SystemApi.SUCCESS;
    }
}