package io.tchepannou.enigma.oms.service.payment;

public class SandboxPaymentService implements PaymentService {
    public static final String FAIL_PHONE = "99501111";

    @Override
    public PaymentResponse pay(final PaymentRequest request) throws PaymentException {
        if (FAIL_PHONE.equals(request.getMobileNumber())){
            throw new PaymentException("Failure");
        }

        return new PaymentResponse(String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public RefundResponse refund(final RefundRequest request) throws PaymentException {
        return new RefundResponse(String.valueOf(System.currentTimeMillis()));
    }
}
