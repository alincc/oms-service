package io.tchepannou.enigma.oms.service.sms;

public class NullSmsGateway implements SmsGateway {

    @Override
    public SendSmsResponse send(final SendSmsRequest request) {
        return new SendSmsResponse();
    }
}
