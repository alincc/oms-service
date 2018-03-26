package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
import io.tchepannou.enigma.refdata.client.SiteBackend;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TicketSmsSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketSmsSender.class);

    @Autowired
    private KVLogger logger;

    @Autowired
    private TicketSmsGenerator messageGenerator;

    @Autowired
    private SiteBackend siteBackend;

    @Autowired
    private SmsGateway gateway;

    public String send(final Ticket ticket) {
        final SiteDto site = siteBackend.findById(ticket.getOrderLine().getOrder().getSiteId()).getSite();

        final String senderId = site.getSmsSenderId();
        final String mobileNumber = ticket.getOrderLine().getOrder().getMobileNumber();
        final String message = messageGenerator.generate(ticket);
        logger.add("SmsSenderId", senderId);
        logger.add("SmsNumber", mobileNumber);
        logger.add("SmsMessage", message);
        logger.add("SmsGateway", gateway.getClass());

        try {

            final String result = gateway.send(senderId, mobileNumber, message);
            logger.add("SmsTransactionID", result);
            return result;

        } catch (RuntimeException e){

            logger.add("SmsException", e.getClass().getName());
            logger.add("SmsExceptionMessage", e.getMessage());

            LOGGER.error("Unable to send message to {}", mobileNumber, e);
            throw e;
        }
    }
}
