package io.tchepannou.enigma.oms.service;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.exception.HttpStatusException;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.oms.backend.ferari.BookingBackend;
import io.tchepannou.enigma.oms.backend.ferari.FerrariException;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.client.rr.GetOrderResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.oms.exception.OrderException;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.service.mq.QueueNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("enigma.service.order")
public class OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private KVLogger kv;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private Mapper mapper;

    @Autowired
    private BookingBackend ferari;

    private int orderTTLMinutes;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional
    public CreateOrderResponse create(final CreateOrderRequest request){
        try {

            // Create order
            final List<OrderLine> lines = new ArrayList();
            final Order order = mapper.toOrder(request, orderTTLMinutes);
            for (final OfferLineDto dto : request.getOfferLines()){
                OrderLine line = mapper.toOrderLine(dto, order);
                lines.add(line);
            }

            // Save
            orderRepository.save(order);
            for (final OrderLine line : lines){
                orderLineRepository.save(line);
            }
            order.setLines(lines);

            // Log
            kv.add("OrderID", order.getId());
            kv.add("OrderStatus", order.getStatus().name());
            kv.add("Action", "Create");

            // Response
            return new CreateOrderResponse(order.getId());

        } catch (InvalidCarOfferTokenException e){

            throw new OrderException(e, OMSErrorCode.MALFORMED_OFFER_TOKEN);

        }
    }

    @Transactional(noRollbackFor = OrderException.class)
    public CheckoutOrderResponse checkout(
            final Integer orderId,
            final String deviceUID,
            final CheckoutOrderRequest request
    ){
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }
        if (order.isExpired()){
            throw new OrderException(OMSErrorCode.ORDER_EXPIRED);
        }
        if (order.isCancelled()){
            throw new OrderException(OMSErrorCode.ORDER_CANCELLED);
        }

        // Customer information
        order.setDeviceUID(deviceUID);
        order.setCustomerId(request.getCustomerId());
        order.setFirstName(request.getFirstName());
        order.setLastName(request.getLastName());
        order.setEmail(request.getEmail());
        order.setLanguageCode(request.getLanguageCode());

        // Save travellers
        for (final TravellerDto traveller : request.getTravellers()){
            final Traveller obj = mapper.toTraveller(traveller, order);
            travellerRepository.save(obj);
        }

        // Book
        book(order);

        // Apply charges
        charge(order, request);

        // Confirm order
        confirm(order);

        // Log
        kv.add("OrderID", order.getId());
        kv.add("OrderStatus", order.getStatus().name());
        kv.add("Action", "Checkout");

        // Saved order
        return new CheckoutOrderResponse(order.getId());

    }

    @Transactional
    public void expire (final Integer id, final RestClient rest){
        final Order order = orderRepository.findOne(id);
        if (id == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }

        if (OrderStatus.PENDING.equals(order.getStatus())) {
            // Expire the booking
            ferari.expire(order, rest);
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
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

    public void notify(final Integer orderId){
        rabbitTemplate.convertAndSend(QueueNames.EXCHANGE_NEW_ORDER, "", orderId);
    }


    private void book(final Order order){
        if (OrderStatus.PENDING.equals(order)){
            // Do not book pending request
            LOGGER.info("Order#{} has already been booked", order.getId());
            return;
        }

        try {
            final List<BookingDto> bookings = ferari.book(order);

            // Book
            for (int i = 0; i < bookings.size(); i++) {
                final BookingDto dto = bookings.get(i);
                final OrderLine line = order.getLines().get(i);

                line.setBookingId(dto.getId());
            }

            order.setStatus(OrderStatus.PENDING);
            orderRepository.save(order);
        } catch (FerrariException e){
            throw toOrderException(e, OMSErrorCode.BOOKING_FAILURE);
        }
    }

    private void confirm(final Order order){
        if (OrderStatus.CONFIRMED.equals(order)){
            // Do not book pending request
            LOGGER.info("Order#{} has already been confirmed", order.getId());
            return;
        }

        try {
            ferari.confirm(order);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        } catch (FerrariException e){
            throw toOrderException(e, OMSErrorCode.CONFIRM_FAILURE);
        }
    }

    private void charge(final Order order, final CheckoutOrderRequest request){
        if (OrderStatus.PAID.equals(order)){
            // Do not pay PAID request
            LOGGER.info("Order#{} has already been charged", order.getId());
            return;
        }

//            final Integer transactionId = tontine.charge(order, request);
        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setPaymentId(-1);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    private OrderException toOrderException(Throwable e, OMSErrorCode code){
        final Throwable cause = e.getCause();
        if (cause instanceof HttpStatusException){
            final String body = ((HttpStatusException)cause).getResponse().getBody();
            return new OrderException(body, e, code);
        }
        return new OrderException(e, code);
    }

    public int getOrderTTLMinutes() {
        return orderTTLMinutes;
    }

    public void setOrderTTLMinutes(final int orderTTLMinutes) {
        this.orderTTLMinutes = orderTTLMinutes;
    }
}
