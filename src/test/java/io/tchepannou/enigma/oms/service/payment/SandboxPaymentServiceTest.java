package io.tchepannou.enigma.oms.service.payment;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SandboxPaymentServiceTest {
    @Test
    public void pay() throws Exception {
        final PaymentRequest request = new PaymentRequest();
        request.setMobileNumber("43094039");
        request.setProvider("MTN");

        final PaymentResponse result = new SandboxPaymentService().pay(request);
        assertThat(result.getTransactionId()).isNotNull();
    }

    @Test(expected = PaymentException.class)
    public void payFailure() throws Exception {
        final PaymentRequest request = new PaymentRequest();
        request.setMobileNumber(SandboxPaymentService.FAIL_PHONE);
        request.setProvider("MTN");

        new SandboxPaymentService().pay(request);
    }
}
