package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.domain.OrderLine;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.service.sms.SendSmsRequest;
import io.tchepannou.enigma.oms.service.sms.SendSmsResponse;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
import io.tchepannou.enigma.refdata.client.SiteBackend;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import io.tchepannou.enigma.refdata.client.rr.SiteResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
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

        final SendSmsResponse response = new SendSmsResponse();
        response.setMessageId("123");
        when(gateway.send(any())).thenReturn(response);

        // When
        String result = service.send(ticket);

        // Then
        assertThat(result).isEqualTo("123");

        final ArgumentCaptor<SendSmsRequest> request = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(gateway).send(request.capture());
        assertThat(request.getValue().getMessage()).isEqualTo("This is a message");
        assertThat(request.getValue().getPhone()).isEqualTo("5147550101");
        assertThat(request.getValue().getSenderId()).isEqualTo("Test");
    }

    @Test
    public void shouldLogMessage() throws Exception {
        // Given
        final SiteDto site = createSite(1, "Test");
        when(siteBackend.findById(1)).thenReturn(new SiteResponse(site));

        final Ticket ticket = createTicket("5147550101", 1);
        when(generator.generate(ticket)).thenReturn("This is a message");

        final SendSmsResponse response = new SendSmsResponse();
        response.setMessageId("12345");
        when(gateway.send(any())).thenReturn(response);

        // When
        service.send(ticket);

        // When
        verify(logger).add("SmsSenderId", "Test");
        verify(logger).add("SmsNumber", "5147550101");
        verify(logger).add("SmsMessage", "This is a message");
        verify(logger).add("SmsMessageId", "12345");
    }

    @Test
    public void shouldLogError() throws Exception {
        // Given
        final SiteDto site = createSite(1, "Test");
        when(siteBackend.findById(1)).thenReturn(new SiteResponse(site));

        final Ticket ticket = createTicket("5147550101", 1);
        when(generator.generate(ticket)).thenReturn("This is a message");

        Exception ex = new RuntimeException("test");
        when(gateway.send(any())).thenThrow(ex);

        try {
            // When
            service.send(ticket);
            fail("failed");
        } catch (RuntimeException e) {
            // When
            verify(logger).add("SmsSenderId", "Test");
            verify(logger).add("SmsNumber", "5147550101");
            verify(logger).add("SmsMessage", "This is a message");
            verify(logger, never()).add(eq("SmsMessageId"), anyString());
            verify(logger).add("SmsException", RuntimeException.class.getName());
            verify(logger).add("SmsExceptionMessage", "test");
        }
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
