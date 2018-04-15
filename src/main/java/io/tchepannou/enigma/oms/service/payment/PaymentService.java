package io.tchepannou.enigma.oms.service.payment;

public interface PaymentService {
    PaymentResponse pay(PaymentRequest request);
}
