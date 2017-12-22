package io.tchepannou.enigma.oms.service.tontine;

import io.tchepannou.enigma.oms.domain.MobilePayment;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.tontine.client.dto.MobilePaymentInfoDto;
import io.tchepannou.enigma.tontine.client.dto.PaymentInfoDto;
import io.tchepannou.enigma.tontine.client.rr.ChargeRequest;
import org.springframework.stereotype.Component;

@Component
public class TontineMapper {
    public ChargeRequest toChargeRequest(final Order order){
        final ChargeRequest request = new ChargeRequest();
        request.setInvoiceId(order.getId());
        request.setCurrencyCode(order.getCurrencyCode());
        request.setAmount(order.getTotalAmount());
        request.setMerchantId(order.getMerchantId());

        MobilePayment mobile = order.getMobilePayment();
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
