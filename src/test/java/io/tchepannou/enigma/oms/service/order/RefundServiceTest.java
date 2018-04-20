package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.ferari.client.Direction;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefundServiceTest {
    @Mock
    private Clock clock;

    @InjectMocks
    private RefundService calculator;

    @Test
    public void computeRefundAmount() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        OrderLine line1 = createOrderLine(1, createOfferToken(1, 2, 100d, DateUtils.addDays(new Date(now), 2)));
        OrderLine line2 = createOrderLine(1, createOfferToken(2, 1, 300d, DateUtils.addDays(new Date(now), 10)));
        Order order = createOrder(line1, line2);

        calculator.setFreeCancellationHours(24);

        // When
        BigDecimal amount = calculator.computeRefundAmount(order);

        // Then
        assertThat(amount).isEqualTo(new BigDecimal(400d));
    }


    @Test
    public void computeRefundAmountBelowThreshold() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        OrderLine line1 = createOrderLine(1, createOfferToken(1, 2, 100d, DateUtils.addHours(new Date(now), 2)));
        OrderLine line2 = createOrderLine(1, createOfferToken(2, 1, 300d, DateUtils.addDays(new Date(now), 10)));
        Order order = createOrder(line1, line2);

        calculator.setFreeCancellationHours(24);

        // When
        BigDecimal amount = calculator.computeRefundAmount(order);

        // Then
        assertThat(amount).isEqualTo(BigDecimal.ZERO);
    }



    private Order createOrder(OrderLine...lines){
        Order order = new Order();
        order.setLines(Arrays.asList(lines));
        return order;
    }

    private TransportationOfferToken createOfferToken(
            Integer originId,
            Integer destinationId,
            Double unitPrice,
            Date departureDateTime
    ){
        final TransportationOfferToken token = new TransportationOfferToken();

        token.setDirection(Direction.OUTBOUND);
        token.setOriginId(originId);
        token.setAmount(new BigDecimal(unitPrice));
        token.setDestinationId(destinationId);
        token.setArrivalDateTime(new Date());
        token.setCurrencyCode("XAF");
        token.setDepartureDateTime(departureDateTime);
        token.setExpiryDateTime(DateUtils.addDays(new Date(), 1));
        token.setPriceId(1);
        token.setProductId(1);
        token.setTravellerCount(1);
        return token;
    }

    private OrderLine createOrderLine(Integer id, TransportationOfferToken token){
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setQuantity(1);
        line.setMerchantId(1);
        line.setUnitPrice(token.getAmount());
        line.setTotalPrice(token.getAmount());
        line.setOfferToken(token.toString());
        return line;
    }


}
