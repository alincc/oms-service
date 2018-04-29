package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.rest.RestClient;
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
import io.tchepannou.enigma.oms.client.OrderLineType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.TransactionType;
import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.dto.TransactionDto;
import io.tchepannou.enigma.oms.client.exception.NotFoundException;
import io.tchepannou.enigma.oms.client.exception.OrderException;
import io.tchepannou.enigma.oms.client.rr.CancelOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.client.rr.ExpireOrderResponse;
import io.tchepannou.enigma.oms.client.rr.RefundOrderResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.repository.FeesRepository;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.oms.service.payment.PaymentException;
import io.tchepannou.enigma.oms.service.payment.PaymentRequest;
import io.tchepannou.enigma.oms.service.payment.PaymentResponse;
import io.tchepannou.enigma.oms.service.payment.PaymentService;
import io.tchepannou.enigma.oms.service.payment.RefundRequest;
import io.tchepannou.enigma.oms.service.payment.RefundResponse;
import io.tchepannou.enigma.oms.service.ticket.TicketService;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
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
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

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
@SuppressWarnings("CPD-START")
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

    @Mock
    private RefundService refundCalculator;

    @Mock
    private FeesRepository feesRepository;


    @InjectMocks
    private OrderService service;


    @Before
    public void setUp(){
        when(feesRepository.findBySiteId(anyInt())).thenReturn(Collections.emptyList());
    }

    @Test
    public void shouldCreateOrder(){
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        OfferLineDto offer1 = createOfferLine(1, createOfferToken(1, 2, 100d));
        OfferLineDto offer2 = createOfferLine(2, createOfferToken(2, 1, 90d));
        CreateOrderRequest request = createOrderRequest(offer1, offer2);

        Order order = createOrder(1, OrderStatus.NEW, 100d);
        OrderLine orderLine1 = createOrderLine(1, 100d);
        OrderLine orderLine2 = createOrderLine(2, 90d);
        when (mapper.toOrder(request)).thenReturn(order);
        when(mapper.toOrderLine(offer1, order)).thenReturn(orderLine1);
        when(mapper.toOrderLine(offer2, order)).thenReturn(orderLine2);

        Date freeCancellationDate = DateUtils.addDays(new Date(), 1);
        when(refundCalculator.computeFreeCancellationDateTime(order)).thenReturn(freeCancellationDate);

        // When
        service.setOrderTTLMinutes(10);
        CreateOrderResponse response = service.create(request);

        // Then
        assertThat(response.getOrderId()).isEqualTo(1);

        assertThat(order.getCreationDateTime().getTime()).isEqualTo(now);
        assertThat(order.getExpiryDateTime()).isEqualTo(DateUtils.addMinutes(new Date(now), 10));
        assertThat(order.getFreeCancellationDateTime()).isEqualTo(freeCancellationDate);
        assertThat(order.getSubTotalAmount()).isEqualTo(new BigDecimal(190));
        assertThat(order.getTotalFees()).isEqualTo(new BigDecimal(0));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal(190));
        assertThat(order.getCurrencyCode()).isEqualTo("XAF");

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
        assertThat(order.getMobileProvider()).isEqualTo("MTN");
        assertThat(order.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(order.getLastName()).isEqualTo(request.getLastName());
        assertThat(order.getEmail()).isEqualTo(request.getEmail());
        assertThat(order.getCustomerId()).isEqualTo(request.getCustomerId());
        assertThat(order.getCheckoutDateTime().getTime()).isEqualTo(now);

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
        assertThat(paymentRequest.getValue().getMobileNumber()).isEqualTo("23799505678");
        assertThat(paymentRequest.getValue().getMobileProvider()).isEqualTo(request.getMobilePayment().getProvider());
        assertThat(paymentRequest.getValue().getUssdCode()).isEqualTo(request.getMobilePayment().getUssdCode());

        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getAmount()).isEqualTo(order.getTotalAmount());
        assertThat(tx.getValue().getCurrencyCode()).isEqualTo(order.getCurrencyCode());
        assertThat(tx.getValue().getType()).isEqualTo(TransactionType.CHARGE);
        assertThat(tx.getValue().getGatewayTid()).isEqualTo(paymentResponse.getTransactionId());
        assertThat(tx.getValue().getOrder()).isEqualTo(order);
        assertThat(tx.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(tx.getValue().getTransactionDateTime()).isEqualTo(new Date(now));
    }


    @Test
    public void checkoutConfirmedOrder(){
        // Given
        long now = 309403943;
        when(clock.millis()).thenReturn(now);

        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.CONFIRMED, 100d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        TicketDto tick = mock(TicketDto.class);
        when(tick.getId()).thenReturn(11);
        when(ticketService.findByOrder(order)).thenReturn(Arrays.asList(tick));

        Transaction tx = createTransaction(111, order, TransactionType.CHARGE, 100d);
        when(transactionRepository.findByOrderAndType(any(), any())).thenReturn(tx);
        TransactionDto txDto = new TransactionDto();
        txDto.setId(111);
        when(mapper.toDto(tx)).thenReturn(txDto);

        // When
        CheckoutOrderRequest request = checkoutOrderRequest();
        CheckoutOrderResponse response =  service.checkout(1, "4304309", request);

        // Then
        verify(orderRepository, never()).save(order);
        assertThat(response.getOrderId()).isEqualTo(1);
        assertThat(response.getTickets()).hasSize(1);
        assertThat(response.getTickets().get(0).getId()).isEqualTo(11);
        assertThat(response.getTransaction().getId()).isEqualTo(111);

        verify(bookingBackend, never()).book(any());
        verify(orderLineRepository, never()).save(line);

        verify(bookingBackend, never()).confirm(anyInt());

        verify(ticketService, never()).create(order);

        verify(paymentService, never()).pay(any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    public void checkoutShouldNotChangeTwice(){
        // Given
        long now = 309403943;
        when(clock.millis()).thenReturn(now);

        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.NEW, 100d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        BookingDto booking = new BookingDto();
        booking.setId(11);
        when(bookingBackend.book(any())).thenReturn(new CreateBookingResponse(Arrays.asList(booking)));

        Transaction tx = createTransaction(1, order, TransactionType.CHARGE, 100);
        when(transactionRepository.findByOrderAndType(order, TransactionType.CHARGE)).thenReturn(tx);

        TicketDto tick = mock(TicketDto.class);
        when(ticketService.create(order)).thenReturn(Arrays.asList(tick));

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
        assertThat(order.getCheckoutDateTime().getTime()).isEqualTo(now);

        verify(bookingBackend).book(any());
        verify(orderLineRepository).save(line);
        assertThat(line.getBookingId()).isEqualTo(booking.getId());

        verify(bookingBackend).confirm(anyInt());

        verify(ticketService).create(order);
        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.getTickets()).containsExactly(tick);

        verify(paymentService, never()).pay(any());
        verify(transactionRepository, never()).save(any(Transaction.class));
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
    public void shouldNotCheckoutWhenPaymentFail(){
        // Given
        OrderLine line = createOrderLine(1, 100d);
        Order order = createOrder(1, OrderStatus.NEW, 100d, line);
        when(orderRepository.findOne(1)).thenReturn(order);

        BookingDto booking = new BookingDto();
        booking.setId(11);
        when(bookingBackend.book(any())).thenReturn(new CreateBookingResponse(Arrays.asList(booking)));

        TicketDto ticket = mock(TicketDto.class);
        when(ticketService.create(order)).thenReturn(Arrays.asList(ticket));


        when(paymentService.pay(any())).thenThrow(PaymentException.class);

        // When
        try {
            service.checkout(1, "4304309", checkoutOrderRequest());
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.PAYMENT_FAILURE);

            verify(bookingBackend).book(any());
            verify(ticketService, never()).create(order);
        }
    }



    @Test
    public void cancel(){
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final OrderLine line1 = createOrderLine(1, 111, 1000);
        final OrderLine line2 = createOrderLine(1, 222, 1000);
        final Order order = createOrder(11, OrderStatus.CONFIRMED, 1000, line1, line2);
        when(orderRepository.findOne(11)).thenReturn(order);

        final CancelOrderRequest request = cancelOrderRequest();

        // When
        service.cancel(11, request);

        // Then
        verify(ticketService).cancelByBooking(111);
        verify(ticketService).cancelByBooking(222);

        verify(orderRepository).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationDateTime().getTime()).isEqualTo(now);
    }

    @Test(expected = NotFoundException.class)
    public void cancelShouldThrowNotFoundOnInvalidOrder(){
        // When
        service.cancel(99999, cancelOrderRequest());
    }

    @Test(expected = OrderException.class)
    public void cancelShouldFailedForNewOrder(){
        // Given
        final Order order = createOrder(11, OrderStatus.NEW, 1000);
        when(orderRepository.findOne(11)).thenReturn(order);

        final CancelOrderRequest request = cancelOrderRequest();

        // When
        service.cancel(11, request);
    }

    @Test
    public void cancelShouldFailedWithInvalidMobileNumber(){
        // Given
        final Order order = createOrder(11, OrderStatus.CONFIRMED, 1000);
        when(orderRepository.findOne(11)).thenReturn(order);

        final CancelOrderRequest request = cancelOrderRequest();
        request.getMobilePayment().setNumber("invalid");

        try {
            // When
            service.cancel(11, request);
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_INVALID_MOBILE_NUMBER);
        }
    }

    @Test
    public void cancelShouldFailedWithInvalidMobileProvider(){
        // Given
        final Order order = createOrder(11, OrderStatus.CONFIRMED, 1000);
        when(orderRepository.findOne(11)).thenReturn(order);

        final CancelOrderRequest request = cancelOrderRequest();
        request.getMobilePayment().setProvider("invalid");

        try {
            // When
            service.cancel(11, request);
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_INVALID_MOBILE_PROVIDER);
        }
    }

    @Test(expected = OrderException.class)
    public void cancelShouldFailedForCancelledOrder(){
        // Given
        final Order order = createOrder(11, OrderStatus.CANCELLED, 1000);
        when(orderRepository.findOne(11)).thenReturn(order);

        final CancelOrderRequest request = cancelOrderRequest();

        // When
        service.cancel(11, request);
    }


    @Test
    public void onCancelBookings() {
        // Given
        OrderLine line1 = createOrderLine(1, 11, 100);
        OrderLine line2 = createOrderLine(2, 22, 140);
        Order order = createOrder(111, OrderStatus.CANCELLED, 240, line1, line2);
        when(orderRepository.findOne(111)).thenReturn(order);

        BookingBackend backend = mock(BookingBackend.class);

        // When
        service.onCancelBookings(111, backend);

        // Then
        verify(backend).cancel(eq(11), any(CancelBookingRequest.class));
        verify(backend).cancel(eq(22), any(CancelBookingRequest.class));
    }

    @Test
    public void onCancelBookingsWithBookingNotFound() {
        // Given
        OrderLine line1 = createOrderLine(1, 11, 100);
        OrderLine line2 = createOrderLine(2, 22, 140);
        Order order = createOrder(111, OrderStatus.CANCELLED, 240, line1, line2);
        when(orderRepository.findOne(111)).thenReturn(order);

        BookingBackend backend = mock(BookingBackend.class);
        BookingException e = new BookingException(FerrariErrorCode.BOOKING_NOT_FOUND);
        when(backend.cancel(eq(11), any())).thenThrow(e);
        when(backend.cancel(eq(22), any())).thenReturn(new CancelBookingResponse());

        // When
        service.onCancelBookings(111, backend);

        // Then
        verify(backend, times(2)).cancel(anyInt(), any(CancelBookingRequest.class));
    }

    @Test
    public void onCancelBookingsWithBookingAlreadyCancelled() {
        // Given
        OrderLine line1 = createOrderLine(1, 11, 100);
        OrderLine line2 = createOrderLine(2, 22, 140);
        Order order = createOrder(111, OrderStatus.CANCELLED, 240, line1, line2);
        when(orderRepository.findOne(111)).thenReturn(order);

        BookingBackend backend = mock(BookingBackend.class);
        BookingException e = new BookingException(FerrariErrorCode.BOOKING_ALREADY_CANCELLED);
        when(backend.cancel(eq(11), any())).thenThrow(e);
        when(backend.cancel(eq(22), any())).thenReturn(new CancelBookingResponse());

        // When
        service.onCancelBookings(111, backend);

        // Then
        verify(backend, times(2)).cancel(anyInt(), any(CancelBookingRequest.class));
    }

    @Test(expected = RuntimeException.class)
    public void onCancelBookingsWithBookingError() {
        // Given
        OrderLine line1 = createOrderLine(1, 11, 100);
        OrderLine line2 = createOrderLine(2, 22, 140);
        Order order = createOrder(111, OrderStatus.CANCELLED, 240, line1, line2);
        when(orderRepository.findOne(111)).thenReturn(order);

        BookingBackend backend = mock(BookingBackend.class);
        when(backend.cancel(eq(11), any())).thenThrow(RuntimeException.class);

        // When
        service.onCancelBookings(111, backend);
    }


    @Test
    public void shouldExpireNewOrder(){
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final OrderLine line1 = createOrderLine(1, 111, 1000);
        final OrderLine line2 = createOrderLine(1, 222, 1000);
        final Order order = createOrder(11, OrderStatus.NEW, 1000, line1, line2);
        order.setExpiryDateTime(DateUtils.addYears(new Date(now), -1));
        when(orderRepository.findOne(11)).thenReturn(order);

        // When
        service.expire(11);

        // Then
        verify(orderRepository).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationDateTime().getTime()).isEqualTo(now);
    }

    @Test
    public void shouldNotExpireInvalidOrder(){
        // Given
        when(orderRepository.findOne(11)).thenReturn(null);

        try {
            // When
            service.expire(11);
            fail("failed");
        } catch (NotFoundException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_NOT_FOUND);
        }
    }

    @Test
    public void shouldNotExpireCancelledOrder(){
        // Given
        final OrderLine line1 = createOrderLine(1, 111, 1000);
        final Order order = createOrder(11, OrderStatus.CANCELLED, 1000, line1);
        when(orderRepository.findOne(11)).thenReturn(order);

        try {
            // When
            service.expire(11);
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_NOT_NEW);
        }
    }

    @Test
    public void shouldNotExpireConfirmedOrder(){
        // Given
        final OrderLine line1 = createOrderLine(1, 111, 1000);
        final Order order = createOrder(11, OrderStatus.CONFIRMED, 1000, line1);
        when(orderRepository.findOne(11)).thenReturn(order);

        try {
            // When
            service.expire(11);
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_NOT_NEW);
        }
    }

    @Test
    public void shouldExpireAll(){
        // Given
        final Order o1 = createOrder(11, OrderStatus.NEW, 1000);
        final Order o2 = createOrder(22, OrderStatus.NEW, 1000);
        final Order o3 = createOrder(33, OrderStatus.NEW, 1000);
        when (orderRepository.findByStatusAndExpiryDateTimeBefore(any(), any())).thenReturn(Arrays.asList(o1, o2, o3));

        RestClient rest = mock(RestClient.class);

        // When
        service.setPort(123);
        service.expire(rest);

        // Then
        verify(rest).get("http://127.0.0.1:123/v1/orders/11/expire", ExpireOrderResponse.class);
        verify(rest).get("http://127.0.0.1:123/v1/orders/22/expire", ExpireOrderResponse.class);
        verify(rest).get("http://127.0.0.1:123/v1/orders/33/expire", ExpireOrderResponse.class);
    }

    @Test
    public void shouldNotFailWhenExpireAll(){
        // Given
        final Order o1 = createOrder(11, OrderStatus.NEW, 1000);
        final Order o2 = createOrder(22, OrderStatus.NEW, 1000);
        final Order o3 = createOrder(33, OrderStatus.NEW, 1000);
        when (orderRepository.findByStatusAndExpiryDateTimeBefore(any(), any())).thenReturn(Arrays.asList(o1, o2, o3));

        RestClient rest = mock(RestClient.class);
        when(rest.get("http://127.0.0.1:123/v1/orders/11/expire", ExpireOrderResponse.class)).thenThrow(RuntimeException.class);

        // When
        service.setPort(123);
        service.expire(rest);

        // Then
        verify(rest).get("http://127.0.0.1:123/v1/orders/11/expire", ExpireOrderResponse.class);
        verify(rest).get("http://127.0.0.1:123/v1/orders/22/expire", ExpireOrderResponse.class);
        verify(rest).get("http://127.0.0.1:123/v1/orders/33/expire", ExpireOrderResponse.class);
    }

    @Test
    public void shouldNotExpireNonExpiredOrder(){
        // Given
        final long now = System.currentTimeMillis();
        when(clock.millis()).thenReturn(now);

        final OrderLine line1 = createOrderLine(1, 111, 1000);
        final Order order = createOrder(11, OrderStatus.NEW, 1000, line1);
        order.setExpiryDateTime(DateUtils.addYears(new Date(now), 1));
        when(orderRepository.findOne(11)).thenReturn(order);

        try {
            // When
            service.expire(11);
            fail("failed");
        } catch (OrderException e){
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_NOT_EXPIRED);
        }
    }

    @Test
    public void refund(){
        // Given
        long now = 1209210932;
        when (clock.millis()).thenReturn(now);

        OrderLine line1 = createOrderLine(11, 101);
        OrderLine line2 = createOrderLine(12, 102);
        Order order = createOrder(1, OrderStatus.CANCELLED, 100, line1, line2);
        when(orderRepository.findOne(1)).thenReturn(order);

        Transaction charge = createTransaction(1, order, TransactionType.CHARGE, 100);
        when(transactionRepository.findByOrderAndType(order, TransactionType.CHARGE)).thenReturn(charge);

        when(refundCalculator.computeRefundAmount(order)).thenReturn(new BigDecimal(100d));

        RefundResponse paymentResponse = new RefundResponse();
        paymentResponse.setGatewayTid(UUID.randomUUID().toString());
        when(paymentService.refund(any())).thenReturn(paymentResponse);

        // When
        service.refund(1);

        // Then
        ArgumentCaptor<RefundRequest> rr = ArgumentCaptor.forClass(RefundRequest.class);
        verify(paymentService).refund(rr.capture());
        assertThat(rr.getValue().getAmount()).isEqualTo(new BigDecimal(100d));
        assertThat(rr.getValue().getCurrencyCode()).isEqualTo(order.getCurrencyCode());
        assertThat(rr.getValue().getMobileNumber()).isEqualTo(order.getMobileNumber());
        assertThat(rr.getValue().getMobileProvider()).isEqualTo(order.getMobileProvider());

        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getTransactionDateTime()).isEqualTo(new Date(now));
        assertThat(tx.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(tx.getValue().getOrder()).isEqualTo(order);
        assertThat(tx.getValue().getGatewayTid()).isEqualTo(paymentResponse.getGatewayTid());
        assertThat(tx.getValue().getType()).isEqualTo(TransactionType.REFUND);
        assertThat(tx.getValue().getAmount()).isEqualTo(new BigDecimal(100d).multiply(new BigDecimal(-1d)));
        assertThat(tx.getValue().getCurrencyCode()).isEqualTo(order.getCurrencyCode());
    }

    @Test
    public void shouldNotRefundTwice(){
        // Given
        OrderLine line1 = createOrderLine(11, 101);
        OrderLine line2 = createOrderLine(12, 102);
        Order order = createOrder(1, OrderStatus.CANCELLED, 100, line1, line2);
        when(orderRepository.findOne(1)).thenReturn(order);

        Transaction charge = createTransaction(456, order, TransactionType.CHARGE, 100);
        when(transactionRepository.findByOrderAndType(order, TransactionType.CHARGE)).thenReturn(charge);

        Transaction refund = createTransaction(789, order, TransactionType.REFUND, -100);
        when(transactionRepository.findByOrderAndType(order, TransactionType.REFUND)).thenReturn(refund);

        when(refundCalculator.computeRefundAmount(order)).thenReturn(new BigDecimal(100d));

        RefundResponse paymentResponse = new RefundResponse();
        paymentResponse.setGatewayTid(UUID.randomUUID().toString());
        when(paymentService.refund(any())).thenReturn(paymentResponse);

        try {
            // When
            service.refund(1);
            fail("failed");

        } catch (OrderException e) {
            // Then
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_ALREADY_REFUNDED);
            verify(paymentService, never()).refund(any());
            verify(transactionRepository, never()).save(any(Transaction.class));
        }
    }

    @Test
    public void shouldNotRefundNonRefundableOrder(){
        // Given
        OrderLine line1 = createOrderLine(11, 101);
        OrderLine line2 = createOrderLine(12, 102);
        Order order = createOrder(1, OrderStatus.CANCELLED, 100, line1, line2);
        when(orderRepository.findOne(1)).thenReturn(order);

        Transaction charge = createTransaction(456, order, TransactionType.CHARGE, 100);
        when(transactionRepository.findByOrderAndType(order, TransactionType.CHARGE)).thenReturn(charge);

        when(refundCalculator.computeRefundAmount(order)).thenReturn(BigDecimal.ZERO);

        RefundResponse paymentResponse = new RefundResponse();
        paymentResponse.setGatewayTid(UUID.randomUUID().toString());
        when(paymentService.refund(any())).thenReturn(paymentResponse);

        try {
            // When
            service.refund(1);
            fail("failed");

        } catch (OrderException e) {
            // Then
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_NOT_ELIGIBLE_FOR_REFUND);
            verify(paymentService, never()).refund(any());
            verify(transactionRepository, never()).save(any(Transaction.class));
        }
    }

    @Test
    public void shouldNotRefundNonOrdersNotCharged(){
        // Given
        OrderLine line1 = createOrderLine(11, 101);
        OrderLine line2 = createOrderLine(12, 102);
        Order order = createOrder(1, OrderStatus.CANCELLED, 100, line1, line2);
        when(orderRepository.findOne(1)).thenReturn(order);

        when(transactionRepository.findByOrderAndType(order, TransactionType.CHARGE)).thenReturn(null);


        try {
            // When
            service.refund(1);
            fail("failed");

        } catch (OrderException e) {
            // Then
            assertThat(e.getErrorCode()).isEqualTo(OMSErrorCode.ORDER_NOT_PAID);
            verify(paymentService, never()).refund(any());
            verify(transactionRepository, never()).save(any(Transaction.class));
        }
    }

    @Test
    public void shouldNotRefundOnPaymentFailure(){
        // Given
        OrderLine line1 = createOrderLine(11, 101);
        OrderLine line2 = createOrderLine(12, 102);
        Order order = createOrder(1, OrderStatus.CANCELLED, 100, line1, line2);
        when(orderRepository.findOne(1)).thenReturn(order);

        Transaction charge = createTransaction(456, order, TransactionType.CHARGE, 100);
        when(transactionRepository.findByOrderAndType(order, TransactionType.CHARGE)).thenReturn(charge);

        when(refundCalculator.computeRefundAmount(order)).thenReturn(BigDecimal.ONE);

        PaymentException e = new PaymentException("failed");
        when(paymentService.refund(any())).thenThrow(e);

        try {
            // When
            service.refund(1);
            fail("failed");

        } catch (OrderException ex) {
            // Then
            assertThat(ex.getErrorCode()).isEqualTo(OMSErrorCode.PAYMENT_FAILURE);
            verify(transactionRepository, never()).save(any(Transaction.class));
        }
    }

    @Test(expected = NotFoundException.class)
    public void refundInvalidOrder(){
        service.refund(123);
    }

    @Test(expected = OrderException.class)
    public void refundInvalidNotCancelledOrder(){
        OrderLine line1 = createOrderLine(11, 1);
        OrderLine line2 = createOrderLine(12, 1);
        Order order = createOrder(1, OrderStatus.CONFIRMED, 2, line1, line2);
        when(orderRepository.findOne(1)).thenReturn(order);

        service.refund(1);
    }


    @Test
    public void onRefund(){
        RestClient rest = mock(RestClient.class);
        service.setPort(123);
        service.onRefund(111, rest);

        verify(rest).get("http://127.0.0.1:123/v1/orders/111/refund", RefundOrderResponse.class);
    }

    @Test
    public void onCancelBookingsInvalidOrder() {
        // Given
        BookingBackend backend = mock(BookingBackend.class);

        // When
        service.onCancelBookings(111, backend);

        // Then
        verify(backend, never()).cancel(any(), any(CancelBookingRequest.class));
    }

    @Test
    public void onCancelBookingsOrderNotCancelled() {
        // Given
        OrderLine line1 = createOrderLine(1, 11, 100);
        OrderLine line2 = createOrderLine(2, 22, 140);
        Order order = createOrder(111, OrderStatus.NEW, 240, line1, line2);
        when(orderRepository.findOne(111)).thenReturn(order);

        BookingBackend backend = mock(BookingBackend.class);

        // When
        service.onCancelBookings(111, backend);

        // Then
        verify(backend, never()).cancel(any(), any(CancelBookingRequest.class));
    }

    private Order createOrder(Integer id, OrderStatus status, double totalPrice, OrderLine...lines){
        final Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal(totalPrice));
        order.setCurrencyCode("XAF");
        order.setSiteId(1);
        order.setMobileProvider("MTN");
        order.setMobileNumber("23799505678");
        if (lines != null) {
            order.setLines(Arrays.asList(lines));
            order.getLines().forEach(line -> line.setOrder(order));
        }
        return order;
    }

    private OrderLine createOrderLine(Integer id, double unitPrice) {
        return createOrderLine(id, null, unitPrice);
    }

    private OrderLine createOrderLine(Integer id, Integer bookingId, double unitPrice){
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setBookingId(bookingId);
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
        line.setType(OrderLineType.CAR);
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
        MobilePaymentDto payment = mobilePayment();

        CheckoutOrderRequest request = new CheckoutOrderRequest();
        request.setLanguageCode("fr");
        request.setCustomerId(1);
        request.setFirstName("Ray");
        request.setLastName("Sponsible");
        request.setEmail("ray.sponsible@gmail.com");
        request.setMobilePayment(payment);
        return request;
    }


    private MobilePaymentDto mobilePayment(){
        MobilePaymentDto payment = new MobilePaymentDto();
        payment.setUssdCode("1232");
        payment.setCountryCode("237");
        payment.setNumber("99505678");
        payment.setProvider("MTN");
        return payment;
    }


    private CancelOrderRequest cancelOrderRequest(){
        CancelOrderRequest req = new CancelOrderRequest();
        req.setMobilePayment(mobilePayment());
        return req;
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


    private Transaction createTransaction(Integer id, Order order, TransactionType type, double amount){
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setType(type);
        tx.setOrder(order);
        tx.setGatewayTid(UUID.randomUUID().toString());
        tx.setAmount(new BigDecimal(amount));
        return tx;
    }
}
