package io.tchepannou.enigma.oms.backend.tontine;

import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.tontine.client.rr.ChargeRequest;
import org.springframework.stereotype.Component;

@Component
public class TontineMapper {
    public ChargeRequest toChargeRequest(final Order order, final CheckoutOrderRequest checkoutRequest){
        final ChargeRequest request = new ChargeRequest();
        request.setInvoiceId(order.getId());
        request.setCurrencyCode(order.getCurrencyCode());
        request.setAmount(order.getTotalAmount());

        MobilePaymentDto mobile = checkoutRequest.getMobilePayment();
        if (mobile != null){
            io.tchepannou.enigma.tontine.client.dto.MobilePaymentDto dto = new io.tchepannou.enigma.tontine.client.dto.MobilePaymentDto();
            dto.setNumber(mobile.getNumber());
            dto.setCountryCode(mobile.getCountryCode());
            dto.setProvider(mobile.getProvider());
            dto.setAreaCode(mobile.getAreaCode());
            dto.setUssdCode(mobile.getUssdCode());

            request.setMobilePayment(dto);
        }
        return request;
    }
}
