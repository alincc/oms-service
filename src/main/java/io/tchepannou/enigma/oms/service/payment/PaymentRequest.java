package io.tchepannou.enigma.oms.service.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    private String firstName;
    private String lastName;
    private String mobilePhone;
    private Double amount;
    private String currency;
    private String ussdCode;
}
