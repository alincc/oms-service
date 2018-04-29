package io.tchepannou.enigma.oms.service.order;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.RestConfig;
import io.tchepannou.core.rest.exception.HttpStatusException;
import io.tchepannou.core.rest.impl.DefaultRestClient;
import io.tchepannou.enigma.ferari.client.CancellationReason;
import io.tchepannou.enigma.ferari.client.FerariEnvironment;
import io.tchepannou.enigma.ferari.client.FerrariErrorCode;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.ferari.client.backend.BookingBackend;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.ferari.client.exception.BookingException;
import io.tchepannou.enigma.ferari.client.rr.CancelBookingRequest;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingRequest;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.TransactionType;
import io.tchepannou.enigma.oms.client.dto.MobilePaymentDto;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.exception.NotFoundException;
import io.tchepannou.enigma.oms.client.exception.OrderException;
import io.tchepannou.enigma.oms.client.rr.CancelOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CancelOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.client.rr.ExpireOrderResponse;
import io.tchepannou.enigma.oms.client.rr.GetOrderResponse;
import io.tchepannou.enigma.oms.client.rr.RefundOrderResponse;
import io.tchepannou.enigma.oms.domain.Fees;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Transaction;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.repository.FeesRepository;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TransactionRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.oms.service.QueueNames;
import io.tchepannou.enigma.oms.service.payment.PaymentException;
import io.tchepannou.enigma.oms.service.payment.PaymentRequest;
import io.tchepannou.enigma.oms.service.payment.PaymentResponse;
import io.tchepannou.enigma.oms.service.payment.PaymentService;
import io.tchepannou.enigma.oms.service.payment.RefundRequest;
import io.tchepannou.enigma.oms.service.payment.RefundResponse;
import io.tchepannou.enigma.oms.service.ticket.TicketService;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties("enigma.service.order")
public class OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private Clock clock;

    @Autowired
    private KVLogger kv;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private Mapper mapper;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundCalculator;

    @Autowired
    private FerariEnvironment ferariEnvironment;

    @Autowired
    private BookingBackend bookingBackend;

    @Autowired
    private FeesRepository feesRepository;

    @Autowired
    private RestClient rest;

    @Value("${server.port}")
    private int port;

    private int orderTTLMinutes;

    //-- Message Handler
    @Transactional
    @RabbitListener(queues = QueueNames.QUEUE_ORDER_REFUND)
    public void onRefund(Integer orderId) {
        try {

            onRefund(orderId, new DefaultRestClient(new RestConfig()));
            LOGGER.error("Order#{} has been refunded", orderId);

        } catch (Exception e){
            LOGGER.error("Unable to refund Order#{}", orderId, e);
        }
    }

    @VisibleForTesting
    protected void onRefund(Integer orderId, RestClient rest){
        final String url = "http://127.0.0.1:" + port + "/v1/orders/" + orderId + "/refund";
        rest.get(url, RefundOrderResponse.class);
    }

    @RabbitListener(queues = QueueNames.QUEUE_BOOKING_CANCEL)
    public void onCancelBookings(Integer orderId) {
        final BookingBackend backend = new BookingBackend(new DefaultRestClient(new RestConfig()), ferariEnvironment);
        onCancelBookings(orderId, backend);
    }

    @VisibleForTesting
    protected void onCancelBookings(Integer orderId, BookingBackend backend) {

        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            LOGGER.warn("Cannot cancel bookings of Order#{}. Order not found", orderId);
            return;
        }
        if (!OrderStatus.CANCELLED.equals(order.getStatus())){
            LOGGER.warn("Cannot cancel bookings of Order#{}. Order.status={}", orderId, order.getStatus());
            return;
        }

        final CancelBookingRequest request = new CancelBookingRequest();
        request.setReason(CancellationReason.OTHER);
        for (OrderLine line : order.getLines()) {
            final Integer bookingId = line.getBookingId();
            if (bookingId == null){
                continue;
            }

            try {

                backend.cancel(bookingId, request);
                LOGGER.info("Booking#{} has been cancelled", bookingId);

            } catch (BookingException e) {

                final FerrariErrorCode code = e.getErrorCode();
                if (FerrariErrorCode.BOOKING_NOT_FOUND.equals(code)){
                    // Ignore
                } else if (FerrariErrorCode.BOOKING_ALREADY_CANCELLED.equals(code)){
                    // Ignore
                } else {
                    throw e;
                }

            } catch (RuntimeException e) {
                LOGGER.error("Unable to cancel Booking#{}", bookingId, e);
                throw e;
            }
        }
    }

    //-- Public
    @Transactional
    public CreateOrderResponse create(final CreateOrderRequest request){
        kv.add("Action", "Create");

        final Date now = new Date(clock.millis());
        try {

            // Create order
            final Order order = mapper.toOrder(request);
            final List<OrderLine> lines = addLines(request, order);
            lines.addAll(applyFees(request, order));
            order.setTotalAmount(order.getSubTotalAmount().add(order.getTotalFees()));

            // Save
            order.setLines(lines);
            order.setCreationDateTime(now);
            order.setFreeCancellationDateTime(refundCalculator.computeFreeCancellationDateTime(order));
            order.setExpiryDateTime(DateUtils.addMinutes(now, orderTTLMinutes));
            orderRepository.save(order);
            for (final OrderLine line : lines){
                orderLineRepository.save(line);
            }

            // Log
            kv.add("OrderID", order.getId());

            // Response
            return new CreateOrderResponse(order.getId());

        } catch (InvalidCarOfferTokenException e){

            throw new OrderException(e, OMSErrorCode.OFFER_MALFORMED_TOKEN);

        }
    }

    @Transactional
    public CheckoutOrderResponse checkout(
            final Integer orderId,
            final String deviceUID,
            final CheckoutOrderRequest request
    ){
        kv.add("OrderID", orderId);
        kv.add("Action", "Checkout");

        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }

        if (OrderStatus.CONFIRMED.equals(order.getStatus())){
            // Do not book pending request
            LOGGER.info("Order#{} has already been confirmed", order.getId());
            Transaction tx = transactionRepository.findByOrderAndType(order, TransactionType.CHARGE);
            return new CheckoutOrderResponse(
                    order.getId(),
                    ticketService.findByOrder(order),
                    mapper.toDto(tx)
            );
        }

        if (OrderStatus.EXPIRED.equals(order.getStatus())){
            throw new OrderException(OMSErrorCode.ORDER_EXPIRED);
        }

        // Customer information
        order.setCheckoutDateTime(new Date(clock.millis()));
        order.setDeviceUID(deviceUID);
        order.setCustomerId(request.getCustomerId());
        order.setFirstName(request.getFirstName());
        order.setLastName(request.getLastName());
        order.setEmail(request.getEmail());
        order.setLanguageCode(request.getLanguageCode());
        order.setMobileNumber(toPhoneText(request.getMobilePayment()));
        order.setMobileProvider(request.getMobilePayment().getProvider());

        // Save travellers
        for (final TravellerDto traveller : request.getTravellers()){
            final Traveller obj = mapper.toTraveller(traveller, order);
            travellerRepository.save(obj);
        }

        // Book
        book(order);

        // Apply charges
        Transaction tx = charge(order, request);

        // Confirm order
        final List<TicketDto> tickets = confirm(order);

        // Saved order
        return new CheckoutOrderResponse(
                order.getId(),
                tickets,
                mapper.toDto(tx)
        );

    }

    @Transactional
    public GetOrderResponse findById(final Integer orderId){
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }

        final GetOrderResponse response = new GetOrderResponse();
        response.setOrder(mapper.toDto(order));
        return response;
    }

    @Transactional
    public CancelOrderResponse cancel(final Integer orderId, final CancelOrderRequest request) {
        kv.add("OrderID", orderId);
        kv.add("Action", "Cancel");

        // Order
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }

        // Check status
        if (OrderStatus.CANCELLED.equals(order.getStatus())){
            throw new OrderException(OMSErrorCode.ORDER_ALREADY_CANCELLED);
        }
        if (!OrderStatus.CONFIRMED.equals(order.getStatus())){
            throw new OrderException(OMSErrorCode.ORDER_NOT_CONFIRMED);
        }

        // Check phone number
        final String mobileNumber = toPhoneText(request.getMobilePayment());
        if (!mobileNumber.equals(order.getMobileNumber())){
            throw new OrderException(OMSErrorCode.ORDER_INVALID_MOBILE_NUMBER);
        }

        // Check mobile provider
        if (!request.getMobilePayment().getProvider().equals(order.getMobileProvider())){
            throw new OrderException(OMSErrorCode.ORDER_INVALID_MOBILE_PROVIDER);
        }

        // Cancel the order
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationDateTime(new Date(clock.millis()));
        orderRepository.save(order);

        // Cancel the tickets
        for (OrderLine line : order.getLines()) {
            ticketService.cancelByBooking(line.getBookingId());
        }


        // Update
        return new CancelOrderResponse(mapper.toDto(order));
    }

    @Transactional
    public ExpireOrderResponse expire(final Integer orderId) {
        kv.add("OrderID", orderId);
        kv.add("Action", "Expire");

        // Order
        final Date now = new Date(clock.millis());
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }

        // Check status
        if (!OrderStatus.NEW.equals(order.getStatus())){
            throw new OrderException(OMSErrorCode.ORDER_NOT_NEW);
        }
        if (order.getExpiryDateTime().after(now)){
            throw new OrderException(OMSErrorCode.ORDER_NOT_EXPIRED);
        }

        // Cancel the order
        order.setStatus(OrderStatus.EXPIRED);
        order.setCancellationDateTime(now);
        orderRepository.save(order);

        // Update
        return new ExpireOrderResponse(mapper.toDto(order));
    }

    @Transactional
    public RefundOrderResponse refund(final Integer orderId) {
        kv.add("OrderID", orderId);
        kv.add("Action", "Refund");

        // Order
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }

        // Check status
        if (!OrderStatus.CANCELLED.equals(order.getStatus())){
            throw new OrderException(OMSErrorCode.ORDER_NOT_CANCELLED);
        }

        final Transaction tx = refund(order);
        kv.add("TransactionID", tx.getId());

        return new RefundOrderResponse(mapper.toDto(tx));
    }

    @Scheduled(cron = "${enigma.service.order.expiry.cron}")
    public void expire() {
        final RestClient rest = new DefaultRestClient(new RestConfig());
        expire(rest);
    }

    @VisibleForTesting
    protected void expire(RestClient rest) {
        final Date now = new Date(clock.millis());
        final List<Order> orders = orderRepository.findByStatusAndExpiryDateTimeBefore(OrderStatus.NEW, now);
        LOGGER.info("{} order(s) to expire", orders.size());

        for (final Order order : orders){
            try {
                LOGGER.info("Expiring Order#{}", order.getId());
                rest.get("http://127.0.0.1:" + this.port + "/v1/orders/" + order.getId() + "/expire", ExpireOrderResponse.class);
            } catch (Exception e){
                LOGGER.error("Unable to expire Order#{}", order.getId(), e);
            }
        }
    }


    //-- Private
    private List<OrderLine> addLines(final CreateOrderRequest request, final Order order) {
        BigDecimal total = BigDecimal.ZERO;
        final List<OrderLine> lines = new ArrayList();
        for (final OfferLineDto dto : request.getOfferLines()){
            OrderLine line = mapper.toOrderLine(dto, order);
            lines.add(line);

            total = total.add(line.getTotalPrice());
        }
        order.setSubTotalAmount(total);
        return lines;
    }

    private List<OrderLine> applyFees(final CreateOrderRequest request, final Order order) {
        BigDecimal total = BigDecimal.ZERO;
        final List<Fees> fees = feesRepository.findBySiteId(request.getSiteId());
        final List<OrderLine> lines = new ArrayList();
        for (final Fees fee : fees){
            final OrderLine line = mapper.toOrderLine(fee, order);
            lines.add(line);

            total = total.add(line.getTotalPrice());
        }
        order.setTotalFees(total);
        return lines;
    }

    private void book(final Order order){
        try {
            CreateBookingRequest request = toCreateBookingRequest(order);
            final List<BookingDto> bookings = bookingBackend.book(request).getBookings();

            // Book
            final List<OrderLine> lines = order.getLines();
            for (int i = 0; i < bookings.size(); i++) {
                final BookingDto dto = bookings.get(i);
                final OrderLine line = lines.get(i);
                line.setBookingId(dto.getId());
                orderLineRepository.save(line);
            }

            orderRepository.save(order);
        } catch (io.tchepannou.enigma.ferari.client.exception.TaggedException e) {

            final FerrariErrorCode code = e.getErrorCode();

            if (FerrariErrorCode.OFFER_EXPIRED.equals(code)){
                throw new OrderException(e, OMSErrorCode.OFFER_EXPIRED);
            } else if (FerrariErrorCode.OFFER_NOT_FOUND.equals(code)){
                throw new OrderException(e, OMSErrorCode.OFFER_NOT_FOUND);
            } else if (FerrariErrorCode.BOOKING_AVAILABILITY_ERROR.equals(code)){
                throw new OrderException(e, OMSErrorCode.BOOKING_AVAILABILITY_ERROR);
            }
        }
    }

    private List<TicketDto> confirm(final Order order){
        try {

            for (OrderLine line : order.getLines()) {
                final Integer bookingId = line.getBookingId();
                if (bookingId != null) {
                    bookingBackend.confirm(bookingId);
                }
            }

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            return ticketService.create(order);

        } catch (HttpStatusException e){
            throw toOrderException(e, OMSErrorCode.CONFIRM_FAILURE);
        }
    }

    private Transaction charge(final Order order, final CheckoutOrderRequest request){
        Transaction tx = transactionRepository.findByOrderAndType(order, TransactionType.CHARGE);
        if (tx != null){
            return tx;
        }

        try {

            // Perform payment
            final PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setMobileProvider(request.getMobilePayment().getProvider());
            paymentRequest.setMobileNumber(toPhoneText(request.getMobilePayment()));
            paymentRequest.setUssdCode(request.getMobilePayment().getUssdCode());
            paymentRequest.setAmount(order.getTotalAmount());
            paymentRequest.setCurrencyCode(order.getCurrencyCode());
            final PaymentResponse paymentResponse = paymentService.pay(paymentRequest);

            // Record transaction
            tx = new Transaction();
            tx.setAmount(paymentRequest.getAmount());
            tx.setCurrencyCode(paymentRequest.getCurrencyCode());
            tx.setGatewayTid(paymentResponse.getTransactionId());
            tx.setOrder(order);
            tx.setTransactionDateTime(new Date(clock.millis()));
            tx.setType(TransactionType.CHARGE);
            tx.setPaymentMethod(PaymentMethod.ONLINE);
            transactionRepository.save(tx);

            return tx;

        } catch (PaymentException e){
            throw new OrderException(e, OMSErrorCode.PAYMENT_FAILURE);
        }
    }

    private Transaction refund (final Order order) {
        Transaction tx = transactionRepository.findByOrderAndType(order, TransactionType.REFUND);
        if (tx != null){
            throw new OrderException(OMSErrorCode.ORDER_ALREADY_REFUNDED);
        }

        final Transaction chargeTx = transactionRepository.findByOrderAndType(order, TransactionType.CHARGE);
        if (chargeTx == null){
            throw new OrderException(OMSErrorCode.ORDER_NOT_PAID);
        }

        final BigDecimal amount = refundCalculator.computeRefundAmount(order);
        if (BigDecimal.ZERO.equals(amount)){
            throw new OrderException(OMSErrorCode.ORDER_NOT_ELIGIBLE_FOR_REFUND);
        }

        try {
            // Perform refund
            final RefundRequest refundRequest = new RefundRequest();
            refundRequest.setMobileNumber(order.getMobileNumber());
            refundRequest.setMobileProvider(order.getMobileProvider());
            refundRequest.setCurrencyCode(order.getCurrencyCode());
            refundRequest.setAmount(amount);
            final RefundResponse refundResponse = paymentService.refund(refundRequest);

            // Save Transaction
            tx = new Transaction();
            tx.setAmount(refundRequest.getAmount().multiply(new BigDecimal(-1d)));
            tx.setCurrencyCode(refundRequest.getCurrencyCode());
            tx.setGatewayTid(refundResponse.getGatewayTid());
            tx.setOrder(order);
            tx.setTransactionDateTime(new Date(clock.millis()));
            tx.setType(TransactionType.REFUND);
            tx.setPaymentMethod(PaymentMethod.ONLINE);
            transactionRepository.save(tx);

            return tx;
        } catch (PaymentException e){
            throw new OrderException(e, OMSErrorCode.PAYMENT_FAILURE);
        }
    }


    private OrderException toOrderException(Throwable e, OMSErrorCode code){
        final Throwable cause = e.getCause();
        if (cause instanceof HttpStatusException){
            final String body = ((HttpStatusException)cause).getResponse().getBody();
            return new OrderException(body, e, code);
        }
        return new OrderException(e, code);
    }

    private CreateBookingRequest toCreateBookingRequest(final Order order){
        final CreateBookingRequest request = new CreateBookingRequest();
        request.setOrderId(order.getId());
        request.setLastName(order.getLastName());
        request.setFirstName(order.getFirstName());
        request.setEmail(order.getEmail());
        request.setOfferTokens(
                order.getLines().stream()
                        .map(l -> l.getOfferToken())
                        .filter(token -> token != null)
                        .collect(Collectors.toList())
        );
        return request;
    }

    private String toPhoneText(MobilePaymentDto payment){
        return Joiner
                .on("")
                .skipNulls()
                .join(
                        payment.getCountryCode(),
                        payment.getAreaCode(),
                        payment.getNumber()
                );
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public int getOrderTTLMinutes() {
        return orderTTLMinutes;
    }

    public void setOrderTTLMinutes(final int orderTTLMinutes) {
        this.orderTTLMinutes = orderTTLMinutes;
    }
}
