package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.Direction;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.oms.backend.ferari.BookingBackend;
import io.tchepannou.enigma.oms.client.OfferType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.oms.exception.OrderException;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.oms.service.ticket.TicketService;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderServiceTest {
    @Mock
    private KVLogger kv;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private TravellerRepository travellerRepository;

    @Mock
    private Mapper mapper;

    @Mock
    private BookingBackend ferari;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private OrderService service;

    @Test
    public void shouldCreateOrder(){
        // Given
        OfferLineDto line1 = createOfferLine(1, createOfferToken(1, 2, 100d));
        OfferLineDto line2 = createOfferLine(2, createOfferToken(2, 1, 90d));
        CreateOrderRequest request = createOrderRequest(line1, line2);

        int ttl = 10;
        service.setOrderTTLMinutes(ttl);
        Order order = createOrder(1, OrderStatus.NEW, 100d);
        OrderLine orderLine1 = mock(OrderLine.class);
        OrderLine orderLine2 = mock(OrderLine.class);
        when (mapper.toOrder(request, ttl)).thenReturn(order);
        when(mapper.toOrderLine(line1, order)).thenReturn(orderLine1);
        when(mapper.toOrderLine(line2, order)).thenReturn(orderLine2);

        // When
        CreateOrderResponse response = service.create(request);

        // Then
        assertThat(response.getOrderId()).isEqualTo(1);

        verify(orderRepository).save(order);
        verify(orderLineRepository).save(orderLine1);
        verify(orderLineRepository).save(orderLine2);
    }

    @Test
    public void shouldCheckout(){
        // Given
        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.NEW, 100d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        BookingDto booking = new BookingDto();
        booking.setId(11);
        when(ferari.book(order)).thenReturn(Arrays.asList(booking));

        TicketDto ticket = mock(TicketDto.class);
        when(ticketService.create(order)).thenReturn(Arrays.asList(ticket));

        // When
        CheckoutOrderRequest request = checkoutOrderRequest();
        CheckoutOrderResponse response =  service.checkout(1, "4304309", request);

        // Then
        verify(orderRepository, times(3)).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getDeviceUID()).isEqualTo("4304309");
        assertThat(order.getLanguageCode()).isEqualTo(request.getLanguageCode());
        assertThat(order.getMobileNumber()).isEqualTo("23799505678");
        assertThat(order.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(order.getLastName()).isEqualTo(request.getLastName());
        assertThat(order.getEmail()).isEqualTo(request.getEmail());
        assertThat(order.getCustomerId()).isEqualTo(request.getCustomerId());
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(order.getPaymentId()).isNotNull();

        verify(ferari).book(order);
        verify(orderLineRepository).save(line);
        assertThat(line.getBookingId()).isEqualTo(booking.getId());

        verify(ferari).confirm(order);

        verify(ticketService).create(order);
        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.getTickets()).containsExactly(ticket);
    }

    @Test
    public void shouldDoNothingOnCheckoutForCheckedOutOder(){
        // Given
        OrderLine line = createOrderLine(1, 77d);
        Order order = createOrder(1, OrderStatus.CONFIRMED, 77d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        TicketDto ticket = mock(TicketDto.class);
        when(ticketService.findByOrder(order)).thenReturn(Arrays.asList(ticket));

        // When
        CheckoutOrderRequest request = checkoutOrderRequest();
        CheckoutOrderResponse response =  service.checkout(1, "4304309", request);

        // Then
        verify(orderRepository, never()).save(order);

        verify(ferari, never()).book(order);
        verify(orderLineRepository, never()).save(line);

        verify(ferari, never()).confirm(order);

        verify(ticketService, never()).create(order);
        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.getTickets()).containsExactly(ticket);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotCheckoutInvalidOrder(){
        service.checkout(999, "4304309", checkoutOrderRequest());
    }

    @Test(expected = OrderException.class)
    public void shouldNotCheckoutCancelledOrder(){
        // Given
        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.NEW, 100d, line);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findOne(1)).thenReturn(order);

        // When
        service.checkout(1, "4304309", checkoutOrderRequest());
    }

    @Test(expected = OrderException.class)
    public void shouldNotCheckoutExpiredOrder(){
        // Given
        OrderLine line = createOrderLine(1, 1d);
        Order order = createOrder(1, OrderStatus.NEW, 1d, line);
        order.setExpiryDateTime(DateUtils.addDays(new Date(), -1));
        when(orderRepository.findOne(1)).thenReturn(order);

        // When
        service.checkout(1, "4304309", checkoutOrderRequest());
    }

    private Order createOrder(Integer id, OrderStatus status, double totalPrice, OrderLine...lines){
        final Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal(totalPrice));
        order.setSiteId(1);
        if (lines != null) {
            order.setLines(Arrays.asList(lines));
            order.getLines().forEach(line -> line.setOrder(order));
        }
        return order;
    }

    private OrderLine createOrderLine(Integer id, double unitPrice){
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setQuantity(1);
        line.setMerchantId(1);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setTotalPrice(new BigDecimal(unitPrice));
        return line;
    }

    private OfferLineDto createOfferLine(Integer merchantId, TransportationOfferToken token){
        OfferLineDto line = new OfferLineDto();
        line.setDescription("this is desc");
        line.setMerchantId(merchantId);
        line.setType(OfferType.CAR);
        line.setToken(token.toString());
        return line;
    }

    private CreateOrderRequest createOrderRequest(OfferLineDto...lines){
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSiteId(1);
        request.setCustomerId(1);
        request.setOfferLines(Arrays.asList(lines));
        return request;
    }

    private CheckoutOrderRequest checkoutOrderRequest(){
        MobilePaymentDto payment = new MobilePaymentDto();
        payment.setUssdCode("1232");
        payment.setCountryCode("237");
        payment.setNumber("99505678");
        payment.setProvider("MTN");

        CheckoutOrderRequest request = new CheckoutOrderRequest();
        request.setLanguageCode("fr");
        request.setCustomerId(1);
        request.setFirstName("Ray");
        request.setLastName("Sponsible");
        request.setEmail("ray.sponsible@gmail.com");
        request.setMobilePayment(payment);
        return request;
    }
    private TransportationOfferToken createOfferToken(
            Integer originId,
            Integer destinationId,
            Double unitPrice
    ){
        final TransportationOfferToken token = new TransportationOfferToken();
        final Calendar departureDate = DateHelper.getCalendar();
        departureDate.set(Calendar.HOUR_OF_DAY, 15);
        departureDate.set(Calendar.MINUTE, 30);

        token.setDirection(Direction.OUTBOUND);
        token.setOriginId(originId);
        token.setAmount(new BigDecimal(unitPrice));
        token.setDestinationId(destinationId);
        token.setArrivalDateTime(new Date());
        token.setCurrencyCode("XAF");
        token.setDepartureDateTime(departureDate.getTime());
        token.setExpiryDateTime(DateUtils.addDays(new Date(), 1));
        token.setPriceId(1);
        token.setProductId(1);
        token.setTravellerCount(1);
        return token;
    }

}
