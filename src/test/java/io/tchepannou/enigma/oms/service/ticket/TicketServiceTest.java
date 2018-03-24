package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.ferari.client.Direction;
import io.tchepannou.enigma.ferari.client.TransportationOfferToken;
import io.tchepannou.enigma.oms.client.dto.TicketDto;
import io.tchepannou.enigma.oms.client.rr.GetTicketResponse;
import io.tchepannou.enigma.oms.client.rr.SendSmsResponse;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.exception.NotFoundException;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.repository.TicketRepository;
import io.tchepannou.enigma.oms.service.Mapper;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceTest {
    @Mock
    private KVLogger kv;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketSmsSender smsSender;

    @Mock
    private Mapper mapper;

    @InjectMocks
    private TicketService service;


    @Test
    public void create() throws Exception {
        // Given
        Date now = new Date();
        Thread.sleep(100);

        TransportationOfferToken offerToken = createOfferToken(Direction.OUTBOUND, 1, 2);
        OrderLine line = createOrderLine(1, offerToken);
        Order order = createOrder(10, line);

        when(mapper.toTicketDto(any())).thenAnswer(inv -> new TicketDto());

        // When
        final List<TicketDto> results = service.create(order);

        // Then
        assertThat(results).hasSize(1);

        ArgumentCaptor<Ticket> ticket = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticket.capture());

        assertThat(ticket.getValue().getOrderLine()).isEqualTo(line);
        assertThat(ticket.getValue().getSequenceNumber()).isEqualTo(1);
        assertThat(ticket.getValue().getLastName()).isEqualTo(order.getLastName());
        assertThat(ticket.getValue().getFirstName()).isEqualTo(order.getFirstName());
        assertThat(ticket.getValue().getMerchantId()).isEqualTo(line.getMerchantId());
        assertThat(ticket.getValue().getProductId()).isEqualTo(offerToken.getProductId());
        assertThat(ticket.getValue().getDepartureDateTime()).isEqualTo(offerToken.getDepartureDateTime());
        assertThat(ticket.getValue().getPrintDateTime()).isAfter(now);
    }

    @Test
    public void createReturn() throws Exception {
        // Given
        Date now = new Date();
        Thread.sleep(100);

        TransportationOfferToken offerToken1 = createOfferToken(Direction.OUTBOUND, 1, 2);
        OrderLine line1 = createOrderLine(1, offerToken1);

        TransportationOfferToken offerToken2 = createOfferToken(Direction.INBOUND, 2, 1);
        OrderLine line2 = createOrderLine(2, offerToken2);

        Order order = createOrder(10, line1, line2);

        when(mapper.toTicketDto(any())).thenAnswer(inv -> new TicketDto());

        // When
        final List<TicketDto> results = service.create(order);

        // Then
        assertThat(results).hasSize(2);

        ArgumentCaptor<Ticket> ticket = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, times(2)).save(ticket.capture());

        assertThat(ticket.getAllValues().get(0).getOrderLine()).isEqualTo(line1);
        assertThat(ticket.getAllValues().get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(ticket.getAllValues().get(0).getLastName()).isEqualTo(order.getLastName());
        assertThat(ticket.getAllValues().get(0).getFirstName()).isEqualTo(order.getFirstName());
        assertThat(ticket.getAllValues().get(0).getMerchantId()).isEqualTo(line1.getMerchantId());
        assertThat(ticket.getAllValues().get(0).getProductId()).isEqualTo(offerToken1.getProductId());
        assertThat(ticket.getAllValues().get(0).getDepartureDateTime()).isEqualTo(offerToken1.getDepartureDateTime());
        assertThat(ticket.getAllValues().get(0).getPrintDateTime()).isAfter(now);


        assertThat(ticket.getAllValues().get(1).getOrderLine()).isEqualTo(line2);
        assertThat(ticket.getAllValues().get(1).getSequenceNumber()).isEqualTo(1);
        assertThat(ticket.getAllValues().get(1).getLastName()).isEqualTo(order.getLastName());
        assertThat(ticket.getAllValues().get(1).getFirstName()).isEqualTo(order.getFirstName());
        assertThat(ticket.getAllValues().get(1).getMerchantId()).isEqualTo(line2.getMerchantId());
        assertThat(ticket.getAllValues().get(1).getProductId()).isEqualTo(offerToken2.getProductId());
        assertThat(ticket.getAllValues().get(1).getDepartureDateTime()).isEqualTo(offerToken2.getDepartureDateTime());
        assertThat(ticket.getAllValues().get(1).getPrintDateTime()).isAfter(now);
    }

    @Test
    public void sms() throws Exception {
        // Given
        Ticket ticket = mock(Ticket.class);
        when(ticketRepository.findOne(1)).thenReturn(ticket);

        when(smsSender.send(ticket)).thenReturn("1232");

        // When
        SendSmsResponse response = service.sms(1);

        // Then
        assertThat(response.getSmsTransactionId()).isEqualTo("1232");
    }

    @Test
    public void onOrderConfirmed() throws Exception {
        // Given
        Order order = createOrder(11);
        when(orderRepository.findOne(11)).thenReturn(order);

        Ticket ticket1 = createTicket(1);
        Ticket ticket2 = createTicket(2);
        when(ticketRepository.findByOrder(order)).thenReturn(Arrays.asList(ticket1, ticket2));

        RestClient rest = mock(RestClient.class);

        // When
        service.setPort(8080);
        service.onOrderConfirmed(11, rest);

        // Verify
        verify(rest).get("http://127.0.0.1:8080/v1/tickets/1/sms", SendSmsResponse.class);
        verify(rest).get("http://127.0.0.1:8080/v1/tickets/2/sms", SendSmsResponse.class);
    }


    @Test
    public void onOrderConfirmedDontSendSMSOnInvalidOrder() throws Exception {
        // Given
        RestClient rest = mock(RestClient.class);

        // When
        service.onOrderConfirmed(11, rest);

        // Verify
        verify(rest, never()).get(any(), any());
    }

    @Test(expected = NotFoundException.class)
    public void smsInvalidTicket() throws Exception {
        service.sms(999);
    }

    @Test
    public void findById() throws Exception {
        // Given
        Order order = new Order();
        OrderLine line = new OrderLine();
        line.setOrder(order);
        Ticket ticket = mock(Ticket.class);
        when(ticket.getOrderLine()).thenReturn(line);
        when(ticketRepository.findOne(1)).thenReturn(ticket);

        TicketDto dto = mock(TicketDto.class);
        when(mapper.toTicketDto(ticket)).thenReturn(dto);

        // When
        final GetTicketResponse response = service.findById(1);

        // Then
        assertThat(response.getTicket()).isEqualTo(dto);
    }

    @Test(expected = NotFoundException.class)
    public void findByIdInvalidTicket() throws Exception {
        service.findById(999);
    }


    private Ticket createTicket(Integer id){
        Ticket ticket = new Ticket();
        ticket.setId(id);
        return ticket;
    }

    private TransportationOfferToken createOfferToken(
            Direction direction,
            Integer originId,
            Integer destinationId
    ){
        final TransportationOfferToken token = new TransportationOfferToken();
        token.setDirection(direction);
        token.setOriginId(originId);
        token.setAmount(new BigDecimal(100d));
        token.setDestinationId(destinationId);
        token.setArrivalDateTime(new Date());
        token.setCurrencyCode("XAF");
        token.setDepartureDateTime(new Date());
        token.setExpiryDateTime(DateUtils.addDays(new Date(), 1));
        token.setPriceId(1);
        token.setProductId(3);
        token.setTravellerCount(1);
        return token;
    }

    private OrderLine createOrderLine(Integer id, final TransportationOfferToken offerToken){
        final OrderLine line = new OrderLine();
        line.setMerchantId(1);
        line.setOfferToken(offerToken.toString());
        line.setId(id);
        line.setQuantity(offerToken.getTravellerCount());

        return line;
    }

    private Order createOrder(Integer id, OrderLine...lines){
        final Order order = new Order ();
        order.setId(id);
        order.setFirstName("Ray");
        order.setLastName("Sponsible");
        if (lines != null) {
            order.setLines(Arrays.asList(lines));
            order.getLines().forEach(line -> line.setOrder(order));
        }
        return order;
    }
}
