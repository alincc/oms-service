package io.tchepannou.enigma.oms.service.order;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.Direction;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.backend.ferari.BookingBackend;
import io.tchepannou.enigma.oms.client.OfferType;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.client.dto.OfferLineDto;
import io.tchepannou.enigma.oms.client.rr.CreateOrderRequest;
import io.tchepannou.enigma.oms.client.rr.CreateOrderResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    private RabbitTemplate rabbitTemplate;

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
        Order order = createOrder(1, OrderStatus.NEW);
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


    private Order createOrder(Integer id, OrderStatus status){
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        return order;
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
