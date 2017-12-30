package io.tchepannou.enigma.oms.service.tontine;

import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.tontine.client.dto.MobilePaymentInfoDto;
import io.tchepannou.enigma.tontine.client.dto.PaymentInfoDto;
import io.tchepannou.enigma.tontine.client.rr.ChargeRequest;
import org.springframework.stereotype.Component;

@Component
public class TontineMapper {
    public ChargeRequest toChargeRequest(final Order order, CheckoutOrderRequest checkoutRequest){
        final ChargeRequest request = new ChargeRequest();
        request.setInvoiceId(order.getId());
        request.setCurrencyCode(order.getCurrencyCode());
        request.setAmount(order.getTotalAmount());

        MobilePaymentInfoDto mobile = checkoutRequest.getPaymentInfo().getMobile();
        if (mobile != null){
            MobilePaymentInfoDto dto = new MobilePaymentInfoDto();
            dto.setNumber(mobile.getNumber());
            dto.setCountryCode(mobile.getCountryCode());
            dto.setProvider(mobile.getProvider());
            dto.setAreaCode(mobile.getAreaCode());

            PaymentInfoDto paymentInfo = new PaymentInfoDto();
            paymentInfo.setMobile(dto);
            request.setPaymentInfo(paymentInfo);
        }
        return request;
    }
}
