package org.ipring.behavioral.chain2;

public class NullValidator extends AbstractValidator<String> {

    @Override
    public void handle0(String req) {
        System.out.println("this = ThreeHandler");
    }
}