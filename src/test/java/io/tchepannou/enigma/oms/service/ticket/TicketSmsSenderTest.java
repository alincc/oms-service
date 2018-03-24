package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TicketSmsSenderTest {
    @Mock
    private KVLogger logger;

    @Mock
    private TicketSmsGenerator generator;

    @Mock
    private SmsGateway gateway;

    @InjectMocks
    private TicketSmsSender service;

    @Test
    public void shouldSendSms() throws Exception {
        // Given
        final Ticket ticket = createTicket("5147550101");
        when(generator.generate(ticket)).thenReturn("This is a message");

        when(gateway.send(any(), any())).thenReturn("123");

        // When
        String result = service.send(ticket);

        // Then
        assertThat(result).isEqualTo("123");
        verify(gateway).send("5147550101", "This is a message");
    }

    @Test
    public void shouldLogMessage() throws Exception {
        // Given
        final Ticket ticket = createTicket("5147550101");
        when(generator.generate(ticket)).thenReturn("This is a message");
        when(gateway.send(any(), any())).thenReturn("12345");

        // When
        service.send(ticket);

        // When
        verify(logger).add("SmsNumber", "5147550101");
        verify(logger).add("SmsMessage", "This is a message");
        verify(logger).add("SmsTransactionID", "12345");
    }

    private Ticket createTicket(final String number){
        final Order order = new Order ();
        order.setMobileNumber(number);

        final OrderLine line = new OrderLine();
        line.setOrder(order);

        final Ticket ticket = new Ticket();
        ticket.setOrderLine(line);
        ticket.setId(1);
        return ticket;
    }

}
