package io.tchepannou.enigma.oms.config;

import io.tchepannou.enigma.oms.service.payment.PaymentService;
import io.tchepannou.enigma.oms.service.payment.SandboxPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfiguration {
    @Bean
    public PaymentService paymentService(){
        return new SandboxPaymentService();
    }
}

