package io.tchepannou.enigma.oms.service;

import io.tchepannou.core.rest.exception.HttpStatusException;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingResponse;
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
import io.tchepannou.enigma.oms.service.ferari.FerariBookingService;
import io.tchepannou.enigma.oms.service.ferari.FerrariException;
import io.tchepannou.enigma.oms.service.tontine.TontineException;
import io.tchepannou.enigma.oms.service.tontine.TontineService;
import io.tchepannou.enigma.tontine.client.rr.ChargeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private OrderRepository orderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private Mapper mapper;

    @Autowired
    private FerariBookingService ferari;

    @Autowired
    private TontineService tontine;

    private int orderTTLMinutes;


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

            // Response
            return new CreateOrderResponse(order.getId());

        } catch (InvalidCarOfferTokenException e){

            throw new OrderException(e, OMSErrorCode.MALFORMED_OFFER_TOKEN);

        }
    }

    @Transactional
    public CheckoutOrderResponse checkout(final Integer orderId, final CheckoutOrderRequest request){
        final Order order = orderRepository.findOne(orderId);
        if (order == null){
            throw new NotFoundException(OMSErrorCode.ORDER_NOT_FOUND);
        }

        // Customer information
        order.setCustomerId(request.getCustomerId());
        order.setFirstName(request.getFirstName());
        order.setLastName(request.getLastName());
        order.setEmail(request.getEmail());

        // Save travellers + custoer
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

        // Saved order
        return new CheckoutOrderResponse(order.getId());

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

    private void book(final Order order){
        try {
            final CreateBookingResponse response = ferari.book(order);
            final List<BookingDto> bookings = response.getBookings();

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
        try {
            ferari.confirm(order);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        } catch (FerrariException e){
            throw toOrderException(e, OMSErrorCode.CONFIRM_FAILURE);
        }
    }

    private void charge(final Order order, final CheckoutOrderRequest request){
        try {
            order.setPaymentMethod(request.getPaymentMethod());

            if (PaymentMethod.ONLINE.equals(request.getPaymentMethod())) {
                ChargeResponse response = tontine.charge(order, request);

                order.setPaymentId(response.getTransactionId());
                order.setStatus(OrderStatus.PAID);
            }

            orderRepository.save(order);
        } catch (TontineException e){
            throw toOrderException(e, OMSErrorCode.PAYMENT_FAILURE);
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
    public int getOrderTTLMinutes() {
        return orderTTLMinutes;
    }

    public void setOrderTTLMinutes(final int orderTTLMinutes) {
        this.orderTTLMinutes = orderTTLMinutes;
    }
}
