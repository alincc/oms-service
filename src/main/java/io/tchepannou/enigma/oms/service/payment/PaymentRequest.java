package io.tchepannou.enigma.oms.service.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequest {
    private String mobileNumber;
    private String mobileProvider;

    private BigDecimal amount;
    private String currencyCode;
    private String ussdCode;
}
