package org.ipring.behavioral.chain4.base;


import org.ipring.enums.SubCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lgj
 * @date 2024/4/17
 */

public abstract class AbstractValidator<T, E> {

    protected List<AbstractValidator<? super T, E>> list;

    protected abstract SubCode handle0(T req, E entry);

    public final SubCode handle(T req, E entry) {
        return this.handle0(req, entry);
    }

    public static class Builder<T, E> {

        protected List<AbstractValidator<? super T, E>> list = new ArrayList<>();

        public List<AbstractValidator<? super T, E>> build() {
            return this.list;
        }

        public Builder<T, E> addHandler(AbstractValidator<? super T, E> handler) {
            list.add(handler);
            return this;
        }
    }
}