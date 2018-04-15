package io.tchepannou.enigma.oms.service.sms;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SandboxSmsGatewayTest {
    @Test
    public void send(){
        final SendSmsRequest request = new SendSmsRequest();
        request.setPhone("43094039");
        request.setMessage("Hello world");

        final SendSmsResponse result = new SandboxSmsGateway().send(request);
        assertThat(result.getMessageId()).isNotNull();
    }

    @Test(expected = SmsException.class)
    public void sendWithFaulure(){
        final SendSmsRequest request = new SendSmsRequest();
        request.setPhone(SandboxSmsGateway.FAIL_PHONE);

        new SandboxSmsGateway().send(request);
    }
}
