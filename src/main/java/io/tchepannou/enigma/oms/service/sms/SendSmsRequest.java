package io.tchepannou.enigma.oms.service.sms;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendSmsRequest {
    private String senderId;
    private String phone;
    private String message;
}
