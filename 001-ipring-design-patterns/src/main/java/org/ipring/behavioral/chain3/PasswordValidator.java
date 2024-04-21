package org.ipring.behavioral.chain3;

public class PasswordValidator extends AbstractValidator<PasswordValidator.PasswordInte, Object> {

    @Override
    public void handle0(PasswordInte req, Object symbol) {
    }

    public interface PasswordInte {
        Long getAccountId();
    }
}