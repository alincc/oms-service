package io.tchepannou.enigma.oms.service.sms;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NullSmsGatewayTest {
    @Test
    public void send(){
        final SendSmsResponse result = new NullSmsGateway().send(null);
        assertThat(result.getMessageId()).isNull();
    }
}
