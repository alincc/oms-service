package io.tchepannou.enigma.oms.service.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponse {
    private String paymentId;
    private boolean success;
    private String errorCode;
    private String errorMessage;
}
