package io.tchepannou.enigma.oms.config;

import io.tchepannou.enigma.oms.service.sms.AwsSmsGateway;
import io.tchepannou.enigma.oms.service.sms.SandboxSmsGateway;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SmsConfiguration {
    @Bean
    @Profile("!int")
    public SmsGateway localSmsGateway(){
        return new SandboxSmsGateway();
    }

    @Bean
    @Profile("int")
    public SmsGateway smsGateway(){
        return new AwsSmsGateway();
    }
}

