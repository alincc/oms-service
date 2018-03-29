package io.tchepannou.enigma.oms.service.order;

import com.google.common.base.Joiner;
import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.rest.exception.HttpStatusException;
import io.tchepannou.enigma.ferari.client.BookingBackend;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingRequest;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
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
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.oms.service.ticket.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private TicketService ticketService;

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

    @Transactional
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
        if (OrderStatus.CONFIRMED.equals(order.getStatus())){
            // Do not book pending request
            LOGGER.info("Order#{} has already been confirmed", order.getId());
            return new CheckoutOrderResponse(order.getId(), ticketService.findByOrder(order));
        }

        // Customer information
        order.setDeviceUID(deviceUID);
        order.setCustomerId(request.getCustomerId());
        order.setFirstName(request.getFirstName());
        order.setLastName(request.getLastName());
        order.setEmail(request.getEmail());
        order.setLanguageCode(request.getLanguageCode());
        order.setMobileNumber(
                Joiner
                    .on("")
                    .skipNulls()
                    .join(
                        request.getMobilePayment().getCountryCode(),
                        request.getMobilePayment().getAreaCode(),
                        request.getMobilePayment().getNumber()
                    )
        );

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
        final List<TicketDto> tickets = confirm(order);

        // Log
        kv.add("OrderID", order.getId());
        kv.add("OrderStatus", order.getStatus().name());
        kv.add("Action", "Checkout");

        // Saved order
        return new CheckoutOrderResponse(order.getId(), tickets);

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
            CreateBookingRequest request = toCreateBookingRequest(order);
            final List<BookingDto> bookings = ferari.book(request).getBookings();

            // Book
            for (int i = 0; i < bookings.size(); i++) {
                final BookingDto dto = bookings.get(i);
                final OrderLine line = order.getLines().get(i);
                line.setBookingId(dto.getId());
                orderLineRepository.save(line);
            }

            orderRepository.save(order);
        } catch (HttpStatusException e){
            throw toOrderException(e, OMSErrorCode.BOOKING_FAILURE);
        }
    }

    private List<TicketDto> confirm(final Order order){
        try {

            for (OrderLine line : order.getLines()) {
                ferari.confirm(line.getBookingId());
            }

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            return ticketService.create(order);

        } catch (HttpStatusException e){
            throw toOrderException(e, OMSErrorCode.CONFIRM_FAILURE);
        }
    }

    private void charge(final Order order, final CheckoutOrderRequest request){
        if ("411 111 1111".equals(order.getMobileNumber())){
            throw new OrderException(OMSErrorCode.PAYMENT_FAILURE);
        }

        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setPaymentId(23203290);
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

    private CreateBookingRequest toCreateBookingRequest(final Order order){
        final CreateBookingRequest request = new CreateBookingRequest();
        request.setOrderId(order.getId());
        request.setLastName(order.getLastName());
        request.setFirstName(order.getFirstName());
        request.setEmail(order.getEmail());
        request.setOfferTokens(
                order.getLines().stream()
                        .map(l -> l.getOfferToken())
                        .collect(Collectors.toList())
        );
        return request;
    }

}
