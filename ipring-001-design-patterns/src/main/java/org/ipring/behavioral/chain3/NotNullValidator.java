package org.ipring.behavioral.chain3;

public class NotNullValidator extends AbstractValidator<NotNullValidator.NotNullInte, Object> {

    @Override
    public void handle0(NotNullInte req, Object symbol) {

    }

    public interface NotNullInte {
        Long getAccountId();
    }
}