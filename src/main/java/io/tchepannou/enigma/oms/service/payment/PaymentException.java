package io.tchepannou.enigma.oms.service.payment;

public class PaymentException extends RuntimeException{
    public PaymentException(final String message) {
        super(message);
    }

    public PaymentException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
