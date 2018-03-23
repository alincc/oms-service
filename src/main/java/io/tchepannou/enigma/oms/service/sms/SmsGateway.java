package io.tchepannou.enigma.oms.service.sms;

public interface SmsGateway {
    String send(String phone, String message);
}
