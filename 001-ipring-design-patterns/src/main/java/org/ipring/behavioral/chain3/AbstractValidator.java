package org.ipring.behavioral.chain3;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractValidator<T, E> {

    protected List<AbstractValidator<? super T, E>> list;
    protected int current = 0;

    protected abstract void handle0(T req, E entry);

    public final void handle(T req, E entry) {
        this.handle0(req, entry);
        // todo
        if (current < list.size()) {
            list.get(current).handle(req, entry);
            this.current++;
        }
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