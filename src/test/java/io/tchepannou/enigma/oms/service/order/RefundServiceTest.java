package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.enigma.ferari.client.Direction;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.OrderLineType;
import io.tchepannou.enigma.oms.domain.Fees;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("CPD-START")
public class RefundServiceTest {
    @Mock
    private Clock clock;

    @InjectMocks
    private RefundService calculator;

    @Test
    public void computeFreeCancellationDateTime(){
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final OrderLine line1 = createOrderLine(1, 100, DateUtils.addHours(new Date(), 48));
        final OrderLine line2 = createOrderLine(1, 200, DateUtils.addHours(new Date(), 72));
        final Order order = createOrder(DateUtils.addHours(new Date(now), -12), line1, line2);

        calculator.setFreeCancellationHours(24);

        assertThat(calculator.computeFreeCancellationDateTime(order)).isEqualTo(
                DateUtils.addHours(new Date(now), 24)
        );
    }

    @Test
    public void computeFreeCancellationDateTimeWithFees(){
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final Fees fees = createFees(1, 10, false);
        final OrderLine line1 = createOrderLine(1, 100, DateUtils.addHours(new Date(), 48));
        final OrderLine line2 = createOrderLine(1, 200, DateUtils.addHours(new Date(), 72));
        final OrderLine line3 = createOrderLine(1, fees.getAmount().doubleValue(), fees);
        final Order order = createOrder(DateUtils.addHours(new Date(now), -12), line1, line2, line3);

        calculator.setFreeCancellationHours(24);

        assertThat(calculator.computeFreeCancellationDateTime(order)).isEqualTo(
                DateUtils.addHours(new Date(now), 24)
        );
    }

    @Test
    public void computeFreeCancellationDateTimeLastMinute(){
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final OrderLine line1 = createOrderLine(1, 100, DateUtils.addHours(new Date(), 1));
        final OrderLine line2 = createOrderLine(1, 200, DateUtils.addHours(new Date(), 72));
        final Order order = createOrder(DateUtils.addHours(new Date(now), -12), line1, line2);

        calculator.setFreeCancellationHours(24);

        assertThat(calculator.computeFreeCancellationDateTime(order)).isNull();
    }

    @Test
    public void computeRefundAmount() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final OrderLine line1 = createOrderLine(1, 100, DateUtils.addHours(new Date(), 48));
        final OrderLine line2 = createOrderLine(1, 200, DateUtils.addHours(new Date(), 72));
        final Order order = createOrder(DateUtils.addHours(new Date(now), -12), line1, line2);

        calculator.setFreeCancellationHours(24);

        // When
        BigDecimal amount = calculator.computeRefundAmount(order);

        // Then
        assertThat(amount).isEqualTo(new BigDecimal(300d));
    }

    @Test
    public void computeRefundAmountWithNonRefundableFees() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final Fees fees = createFees(1, 10, false);
        final OrderLine line1 = createOrderLine(1, 100, DateUtils.addHours(new Date(), 48));
        final OrderLine line2 = createOrderLine(1, 200, DateUtils.addHours(new Date(), 72));
        final OrderLine line3 = createOrderLine(1, fees.getAmount().doubleValue(), fees);
        final Order order = createOrder(DateUtils.addHours(new Date(now), -12), line1, line2, line3);

        calculator.setFreeCancellationHours(24);

        // When
        BigDecimal amount = calculator.computeRefundAmount(order);

        // Then
        assertThat(amount).isEqualTo(new BigDecimal(300d));
    }

    @Test
    public void computeRefundAmountWithRefundableFees() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final Fees fees = createFees(1, 10, true);
        final OrderLine line1 = createOrderLine(1, 100, DateUtils.addHours(new Date(), 48));
        final OrderLine line2 = createOrderLine(1, 200, DateUtils.addHours(new Date(), 72));
        final OrderLine line3 = createOrderLine(1, fees.getAmount().doubleValue(), fees);
        final Order order = createOrder(DateUtils.addHours(new Date(now), -12), line1, line2, line3);

        calculator.setFreeCancellationHours(24);

        // When
        BigDecimal amount = calculator.computeRefundAmount(order);

        // Then
        assertThat(amount).isEqualTo(new BigDecimal(310d));
    }

    @Test
    public void computeRefundAmountAfterExpiry() throws Exception {
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final OrderLine line1 = createOrderLine(1, 100, DateUtils.addHours(new Date(), 48));
        final OrderLine line2 = createOrderLine(1, 200, DateUtils.addHours(new Date(), 72));
        final Order order = createOrder(DateUtils.addHours(new Date(now), 12), line1, line2);

        calculator.setFreeCancellationHours(24);

        // When
        BigDecimal amount = calculator.computeRefundAmount(order);

        // Then
        assertThat(amount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void computeRefundAmountNoFreeCancellationDate() throws Exception {
        // Given
        final Order order = createOrder(null);

        calculator.setFreeCancellationHours(24);

        // When
        BigDecimal amount = calculator.computeRefundAmount(order);

        // Then
        assertThat(amount).isEqualTo(BigDecimal.ZERO);
    }

    private OrderLine createOrderLine(Integer id, double amount, Date departureDate) {
        TransportationOfferToken token = createOfferToken(1, 2, amount, departureDate);

        OrderLine line = new OrderLine();
        line.setId(id);
        line.setType(OrderLineType.BUS);
        line.setQuantity(1);
        line.setUnitPrice(new BigDecimal(amount));
        line.setTotalPrice(new BigDecimal(amount));
        line.setOfferToken(token.toString());
        return line;
    }

    private OrderLine createOrderLine(Integer id, double amount, Fees fees) {
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setType(OrderLineType.FEES);
        line.setUnitPrice(new BigDecimal(amount));
        line.setQuantity(1);
        line.setTotalPrice(new BigDecimal(amount));
        line.setFees(fees);
        return line;
    }

    private Order createOrder(Date time, OrderLine...lines){
        Order order = new Order();
        order.setFreeCancellationDateTime(time);
        order.setLines(lines == null ? new ArrayList<>() : Arrays.asList(lines));
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

    private Fees createFees(Integer id, double amount, boolean refundable){
        Fees fees = new Fees();
        fees.setId(id);
        fees.setAmount(new BigDecimal(amount));
        fees.setRefundable(refundable);
        return fees;
    }
}
