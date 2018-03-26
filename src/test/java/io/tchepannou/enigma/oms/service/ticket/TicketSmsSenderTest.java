package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
import io.tchepannou.enigma.refdata.client.SiteBackend;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import io.tchepannou.enigma.refdata.client.rr.SiteResponse;
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

    @Mock
    private SiteBackend siteBackend;

    @InjectMocks
    private TicketSmsSender service;

    @Test
    public void shouldSendSms() throws Exception {
        // Given
        final SiteDto site = createSite(1, "Test");
        when(siteBackend.findById(1)).thenReturn(new SiteResponse(site));

        final Ticket ticket = createTicket("5147550101", 1);
        when(generator.generate(ticket)).thenReturn("This is a message");

        when(gateway.send(any(), any(), any())).thenReturn("123");

        // When
        String result = service.send(ticket);

        // Then
        assertThat(result).isEqualTo("123");
        verify(gateway).send("Test","5147550101", "This is a message");
    }

    @Test
    public void shouldLogMessage() throws Exception {
        // Given
        final SiteDto site = createSite(1, "Test");
        when(siteBackend.findById(1)).thenReturn(new SiteResponse(site));

        final Ticket ticket = createTicket("5147550101", 1);
        when(generator.generate(ticket)).thenReturn("This is a message");
        when(gateway.send(any(), any(), any())).thenReturn("12345");

        // When
        service.send(ticket);

        // When
        verify(logger).add("SmsSenderId", "Test");
        verify(logger).add("SmsNumber", "5147550101");
        verify(logger).add("SmsMessage", "This is a message");
        verify(logger).add("SmsTransactionID", "12345");
    }

    private SiteDto createSite(final Integer id, final String smsSenderId){
        SiteDto site = new SiteDto();
        site.setSmsSenderId(smsSenderId);
        site.setId(id);
        return site;
    }

    private Ticket createTicket(final String number, final Integer siteId){
        final Order order = new Order ();
        order.setMobileNumber(number);
        order.setSiteId(siteId);

        final OrderLine line = new OrderLine();
        line.setOrder(order);

        final Ticket ticket = new Ticket();
        ticket.setOrderLine(line);
        ticket.setId(1);
        return ticket;
    }

}
