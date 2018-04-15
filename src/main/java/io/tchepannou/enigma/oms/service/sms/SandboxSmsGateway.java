package io.tchepannou.enigma.oms.service.sms;

public class SandboxSmsGateway implements SmsGateway {
    public static final String FAIL_PHONE = "+15144111111";

    @Override
    public SendSmsResponse send(final SendSmsRequest request) {
        if (FAIL_PHONE.equalsIgnoreCase(request.getPhone())){
            throw new SmsException("Failure");
        }

        return new SendSmsResponse(String.valueOf(System.currentTimeMillis()));
    }
}
