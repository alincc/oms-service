package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.CancellationReason;
import io.tchepannou.enigma.ferari.client.Direction;
import io.tchepannou.enigma.ferari.client.FerrariErrorCode;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.ferari.client.backend.BookingBackend;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.ferari.client.exception.BookingException;
import io.tchepannou.enigma.ferari.client.rr.CancelBookingRequest;
import io.tchepannou.enigma.ferari.client.rr.CancelBookingResponse;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingResponse;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OfferType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.TransactionType;
import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.exception.NotFoundException;
import io.tchepannou.enigma.oms.client.exception.OrderException;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.domain.Cancellation;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.repository.CancellationRepository;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.oms.service.payment.PaymentRequest;
import io.tchepannou.enigma.oms.service.payment.PaymentResponse;
import io.tchepannou.enigma.oms.service.payment.PaymentService;
import io.tchepannou.enigma.oms.service.ticket.TicketService;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
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
    private Clock clock;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private CancellationRepository refundRepository;

    @Mock
    private TravellerRepository travellerRepository;

    @Mock
    private Mapper mapper;

    @Mock
    private BookingBackend bookingBackend;

    @Mock
    private TicketService ticketService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService service;


    @Test
    public void shouldCreateOrder(){
        // Given
        OfferLineDto line1 = createOfferLine(1, createOfferToken(1, 2, 100d));
        OfferLineDto line2 = createOfferLine(2, createOfferToken(2, 1, 90d));
        CreateOrderRequest request = createOrderRequest(line1, line2);

        int ttl = 10;
        Order order = createOrder(1, OrderStatus.NEW, 100d);
        OrderLine orderLine1 = mock(OrderLine.class);
        OrderLine orderLine2 = mock(OrderLine.class);
        when (mapper.toOrder(request)).thenReturn(order);
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
        long now = 309403943;
        when(clock.millis()).thenReturn(now);

        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.NEW, 100d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        BookingDto booking = new BookingDto();
        booking.setId(11);
        when(bookingBackend.book(any())).thenReturn(new CreateBookingResponse(Arrays.asList(booking)));

        TicketDto ticket = mock(TicketDto.class);
        when(ticketService.create(order)).thenReturn(Arrays.asList(ticket));

        PaymentResponse paymentResponse = createPaymentResponse("123");
        when(paymentService.pay(any())).thenReturn(paymentResponse);

        // When
        CheckoutOrderRequest request = checkoutOrderRequest();
        CheckoutOrderResponse response =  service.checkout(1, "4304309", request);

        // Then
        verify(orderRepository, times(2)).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getDeviceUID()).isEqualTo("4304309");
        assertThat(order.getLanguageCode()).isEqualTo(request.getLanguageCode());
        assertThat(order.getMobileNumber()).isEqualTo("23799505678");
        assertThat(order.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(order.getLastName()).isEqualTo(request.getLastName());
        assertThat(order.getEmail()).isEqualTo(request.getEmail());
        assertThat(order.getCustomerId()).isEqualTo(request.getCustomerId());

        verify(bookingBackend).book(any());
        verify(orderLineRepository).save(line);
        assertThat(line.getBookingId()).isEqualTo(booking.getId());

        verify(bookingBackend).confirm(anyInt());

        verify(ticketService).create(order);
        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.getTickets()).containsExactly(ticket);

        ArgumentCaptor<PaymentRequest> paymentRequest = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).pay(paymentRequest.capture());
        assertThat(paymentRequest.getValue().getAmount()).isEqualTo(order.getTotalAmount());
        assertThat(paymentRequest.getValue().getCurrencyCode()).isEqualTo(order.getCurrencyCode());
        assertThat(paymentRequest.getValue().getAreaCode()).isEqualTo(request.getMobilePayment().getAreaCode());
        assertThat(paymentRequest.getValue().getMobileNumber()).isEqualTo(request.getMobilePayment().getNumber());
        assertThat(paymentRequest.getValue().getCountryCode()).isEqualTo(request.getMobilePayment().getCountryCode());
        assertThat(paymentRequest.getValue().getProvider()).isEqualTo(request.getMobilePayment().getProvider());
        assertThat(paymentRequest.getValue().getUssdCode()).isEqualTo(request.getMobilePayment().getUssdCode());

        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getAmount()).isEqualTo(order.getTotalAmount());
        assertThat(tx.getValue().getCurrencyCode()).isEqualTo(order.getCurrencyCode());
        assertThat(tx.getValue().getCancellation()).isNull();
        assertThat(tx.getValue().getType()).isEqualTo(TransactionType.CHARGE);
        assertThat(tx.getValue().getGatewayTid()).isEqualTo(paymentResponse.getTransactionId());
        assertThat(tx.getValue().getOrder()).isEqualTo(order);
        assertThat(tx.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(tx.getValue().getTransactionDateTime()).isEqualTo(new Date(now));
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

        verify(bookingBackend, never()).book(any());
        verify(orderLineRepository, never()).save(line);

        verify(bookingBackend, never()).confirm(anyInt());

        verify(ticketService, never()).create(order);
        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.getTickets()).containsExactly(ticket);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotCheckoutInvalidOrder(){
        service.checkout(999, "4304309", checkoutOrderRequest());
    }

    @Test
    public void shouldNotCheckoutExpiredOffer(){
        // Given
        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.NEW, 100d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        final BookingException ex = new BookingException(FerrariErrorCode.OFFER_EXPIRED);
        when(bookingBackend.book(any())).thenThrow(ex);

        // When
        try {
            service.checkout(1, "4304309", checkoutOrderRequest());
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.OFFER_EXPIRED);
        }
    }

    @Test
    public void shouldNotCheckoutUnavailableOffer(){
        // Given
        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.NEW, 100d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        final BookingException ex = new BookingException(FerrariErrorCode.BOOKING_AVAILABILITY_ERROR);
        when(bookingBackend.book(any())).thenThrow(ex);

        // When
        try {
            service.checkout(1, "4304309", checkoutOrderRequest());
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.BOOKING_AVAILABILITY_ERROR);
        }
    }

    @Test
    public void cancel(){
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final Order order = createOrder(11, OrderStatus.CONFIRMED, 1000);
        when(orderRepository.findOne(11)).thenReturn(order);

        final Date departureDate = DateUtils.addDays(new Date(now), 2);
        final BookingDto booking = createBooking(1, order, departureDate, 1000);
        final CancelBookingResponse response = new CancelBookingResponse(booking);
        when(bookingBackend.cancel(anyInt(), any(CancelBookingRequest.class))).thenReturn(response);

        // When
        service.cancel(1);

        // Then
        ArgumentCaptor<CancelBookingRequest> request = ArgumentCaptor.forClass(CancelBookingRequest.class);
        verify(bookingBackend).cancel(eq(1), request.capture());
        assertThat(request.getValue().getReason()).isEqualTo(CancellationReason.OTHER);

        verify(ticketService).cancelByBooking(1);

        ArgumentCaptor<Cancellation> cancellation = ArgumentCaptor.forClass(Cancellation.class);
        verify(refundRepository).save(cancellation.capture());
        assertThat(cancellation.getValue().getCancellationDateTime()).isEqualTo(new Date(now));
        assertThat(cancellation.getValue().getBookingId()).isEqualTo(booking.getId());
        assertThat(cancellation.getValue().getOrder()).isEqualTo(order);
    }

    @Test(expected = NotFoundException.class)
    public void cancelShouldThrowNotFoundOnInvalidBooking(){
        // Given
        io.tchepannou.enigma.ferari.client.exception.NotFoundException ex = new io.tchepannou.enigma.ferari.client.exception.NotFoundException(FerrariErrorCode.BOOKING_NOT_FOUND);
        when(bookingBackend.cancel(any(), any())).thenThrow(ex);

        // When
        service.cancel(1);
    }

    @Test(expected = OrderException.class)
    public void cancelShouldOrderExceptionOnCancelledOrder(){
        // Given
        BookingException ex = new BookingException(FerrariErrorCode.BOOKING_ALREADY_CANCELLED);
        when(bookingBackend.cancel(any(), any())).thenThrow(ex);

        // When
        service.cancel(1);
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

    private BookingDto createBooking(Integer id, Order order, Date departureDate, double amount){
        BookingDto booking = new BookingDto();
        booking.setId(id);
        booking.setOrderId(order.getId());
        booking.setDepartureDateTime(departureDate);
        booking.setTotalPrice(new BigDecimal(amount));
        booking.setCurrencyCode("XAF");
        return booking;
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

    private PaymentResponse createPaymentResponse(String id) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(id);
        return response;
    }
}
