package io.tchepannou.enigma.oms.service.sms;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NullSmsGatewayTest {
    @Test
    public void send(){
        final String result = new NullSmsGateway().send("Ray", "309403", "Hello world");
        assertThat(result).isNull();
    }
}
