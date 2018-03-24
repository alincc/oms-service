package io.tchepannou.enigma.oms.service.sms;

public interface SmsGateway {
    String send(String sender, String phone, String message);
}
