package io.tchepannou.enigma.oms.service.order;

import com.google.common.base.Joiner;
import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.rest.exception.HttpStatusException;
import io.tchepannou.enigma.ferari.client.CancellationReason;
import io.tchepannou.enigma.ferari.client.FerrariErrorCode;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.ferari.client.backend.BookingBackend;
import io.tchepannou.enigma.ferari.client.dto.BookingDto;
import io.tchepannou.enigma.ferari.client.rr.CancelBookingRequest;
import io.tchepannou.enigma.ferari.client.rr.CancelBookingResponse;
import io.tchepannou.enigma.ferari.client.rr.CreateBookingRequest;
import io.tchepannou.enigma.oms.client.OMSErrorCode;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.PaymentMethod;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.dto.TravellerDto;
import io.tchepannou.enigma.oms.client.exception.NotFoundException;
import io.tchepannou.enigma.oms.client.exception.OrderException;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CheckoutOrderResponse;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.client.rr.GetOrderResponse;
import io.tchepannou.enigma.oms.domain.Cancellation;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Traveller;
import io.tchepannou.enigma.oms.repository.OrderLineRepository;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.CancellationRepository;
import io.tchepannou.enigma.oms.repository.TravellerRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import io.tchepannou.enigma.oms.service.ticket.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private CancellationRepository cancellationRepository;

    @Autowired
    private Mapper mapper;

    @Autowired
    private BookingBackend bookingBackend;

    @Autowired
    private TicketService ticketService;

    @Transactional
    public CreateOrderResponse create(final CreateOrderRequest request){
        kv.add("Action", "Create");

        try {

            // Create order
            final List<OrderLine> lines = new ArrayList();
            final Order order = mapper.toOrder(request);
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

    @Transactional
    public io.tchepannou.enigma.oms.client.rr.CancelBookingResponse cancel(final Integer bookingId) {
        kv.add("BookingID", bookingId);
        kv.add("Action", "Cancel");

        try {
            // Cancel the booking
            final CancelBookingRequest request = new CancelBookingRequest();
            request.setReason(CancellationReason.OTHER);
            final CancelBookingResponse response = bookingBackend.cancel(bookingId, request);

            // Cancel the tickets
            ticketService.cancelByBooking(bookingId);

            // Update
            final Cancellation cancellation = createCancellation(response.getBooking());

            kv.add("OrderID", cancellation.getOrder().getId());
            kv.add("CancellationID", cancellation.getId());

            return new io.tchepannou.enigma.oms.client.rr.CancelBookingResponse(
                    cancellation == null ? null : mapper.toDto(cancellation)
            );

        } catch (io.tchepannou.enigma.ferari.client.exception.TaggedException e) {

            final FerrariErrorCode code = e.getErrorCode();

            if (FerrariErrorCode.BOOKING_NOT_FOUND.equals(code)) {
                throw new NotFoundException(e, OMSErrorCode.BOOKING_NOT_FOUND);
            } else if (FerrariErrorCode.BOOKING_ALREADY_CANCELLED.equals(code)) {
                throw new OrderException(e, OMSErrorCode.CANCELLATION_ALREADY_CANCELLED);
            }
            throw e;
        }
    }

    private Cancellation createCancellation(final BookingDto booking) {
        final Order order = orderRepository.findOne(booking.getOrderId());
        final Cancellation cancellation = new Cancellation();
        cancellation.setOrder(order);
        cancellation.setCancellationDateTime(new Date(clock.millis()));
        cancellation.setBookingId(booking.getId());
        cancellationRepository.save(cancellation);

        return cancellation;
    }

    private void book(final Order order){
        try {
            CreateBookingRequest request = toCreateBookingRequest(order);
            final List<BookingDto> bookings = bookingBackend.book(request).getBookings();

            // Book
            for (int i = 0; i < bookings.size(); i++) {
                final BookingDto dto = bookings.get(i);
                final OrderLine line = order.getLines().get(i);
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
                bookingBackend.confirm(line.getBookingId());
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
