package io.tchepannou.enigma.oms.service.tontine;

import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.tontine.client.rr.ChargeRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TontineMapperTest {

    @InjectMocks
    private TontineMapper mapper;

    @Test
    public void toChargeRequest() throws Exception {
        // Given
        final Order order = new Order();
        order.setId(1);
        order.setCurrencyCode("XAF");
        order.setTotalAmount(new BigDecimal(100));
        order.setPaymentMethod(PaymentMethod.ONLINE);

        final MobilePaymentDto payment = new MobilePaymentDto();
        payment.setUssdCode("123");
        payment.setProvider("MTN");
        payment.setNumber("111111");
        payment.setCountryCode("1");

        final CheckoutOrderRequest request =  new CheckoutOrderRequest();
        request.setCustomerId(1);
        request.setMobilePayment(payment);

        // When
        final ChargeRequest result = mapper.toChargeRequest(order, request);

        // Then
        assertThat(result.getMobilePayment().getAreaCode()).isEqualTo(payment.getAreaCode());
        assertThat(result.getMobilePayment().getCountryCode()).isEqualTo(payment.getCountryCode());
        assertThat(result.getMobilePayment().getNumber()).isEqualTo(payment.getNumber());
        assertThat(result.getMobilePayment().getProvider()).isEqualTo(payment.getProvider());
        assertThat(result.getMobilePayment().getUssdCode()).isEqualTo(payment.getUssdCode());

        assertThat(result.getAmount()).isEqualTo(order.getTotalAmount());
        assertThat(result.getCurrencyCode()).isEqualTo(order.getCurrencyCode());
        assertThat(result.getDescription()).isNull();
        assertThat(result.getInvoiceId()).isEqualTo(order.getId());
    }

}
