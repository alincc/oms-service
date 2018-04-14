package io.tchepannou.enigma.oms.service.sms;

public interface SmsGateway {
    SendSmsResponse send(SendSmsRequest request);
}
