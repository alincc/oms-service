package io.tchepannou.enigma.oms.service.sms;

public class SmsException extends RuntimeException{
    public SmsException(final String message) {
        super(message);
    }

    public SmsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
