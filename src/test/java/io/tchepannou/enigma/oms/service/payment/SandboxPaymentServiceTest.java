package io.tchepannou.enigma.oms.service.payment;

import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class SandboxPaymentServiceTest {
    @Test
    public void pay() throws Exception {
        final PaymentRequest request = new PaymentRequest();
        request.setMobileNumber("43094039");
        request.setMobileProvider("MTN");

        final PaymentResponse result = new SandboxPaymentService().pay(request);
        assertThat(result.getTransactionId()).isNotNull();
    }

    @Test(expected = PaymentException.class)
    public void payFailure() throws Exception {
        final PaymentRequest request = new PaymentRequest();
        request.setMobileNumber(SandboxPaymentService.FAIL_PHONE);
        request.setMobileProvider("MTN");

        new SandboxPaymentService().pay(request);
    }

    @Test
    public void refund() throws Exception {
        final RefundRequest request = new RefundRequest();
        request.setMobileProvider("43040394309");
        request.setMobileProvider("MTN");
        request.setAmount(new BigDecimal(100d));
        request.setCurrencyCode("XAF");

        final RefundResponse result = new SandboxPaymentService().refund(request);
        assertThat(result.getGatewayTid()).isNotNull();
    }
}
