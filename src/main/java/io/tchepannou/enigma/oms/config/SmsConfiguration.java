package io.tchepannou.enigma.oms.config;

import io.tchepannou.enigma.oms.service.sms.NullSmsGateway;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmsConfiguration {
    @Bean
    public SmsGateway smsGateway(){
        return new NullSmsGateway();
    }
}
