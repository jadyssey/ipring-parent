package org.ipring.behavioral.chain2;


/**
 * @author lgj
 * @date 2024/4/17
 */
public abstract class AbstractValidator<T> {

    protected AbstractValidator<T> next;

    protected abstract void handle0(T req);

    public final void handle(T req) {
        this.handle0(req);
        if (next != null) {
            next.handle(req);
        }
    }

    public static class Builder<T> {

        private AbstractValidator<T> head;
        private AbstractValidator<T> tail;

        public AbstractValidator<T> build() {
            return this.head;
        }

        public Builder<T> addHandler(AbstractValidator<T> handler) {
            if (this.head == null) {
                this.head = this.tail = handler;
            }
            this.tail.next = handler;
            this.tail = handler;
            return this;
        }
    }
}