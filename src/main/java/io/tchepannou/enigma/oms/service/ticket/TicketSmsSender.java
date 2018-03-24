package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
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
    private SmsGateway gateway;

    public String send(final Ticket ticket) {
        final String mobileNumber = ticket.getOrderLine().getOrder().getMobileNumber();
        final String message = messageGenerator.generate(ticket);
        logger.add("SmsNumber", mobileNumber);
        logger.add("SmsMessage", message);
        logger.add("SmsGateway", gateway.getClass());

        try {

            final String result = gateway.send(mobileNumber, message);
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
