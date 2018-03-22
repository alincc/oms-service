package io.tchepannou.enigma.oms.service.sms;

public class NullSmsGateway implements SmsGateway {

    @Override
    public String send(final String phoneNumber, final String message) {
        return null;
    }
}
