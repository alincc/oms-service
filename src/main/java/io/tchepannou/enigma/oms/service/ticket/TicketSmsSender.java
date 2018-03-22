package io.tchepannou.enigma.oms.service.ticket;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.oms.domain.Ticket;
import io.tchepannou.enigma.oms.service.sms.SmsGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TicketSmsSender {
    @Autowired
    private KVLogger logger;

    @Autowired
    private TicketSmsGenerator messageGenerator;

    @Autowired
    private SmsGateway gateway;

    public void send(final Ticket ticket) {
        final String mobileNumber = ticket.getOrderLine().getOrder().getMobileNumber();
        final String message = messageGenerator.generate(ticket);

        final String result = gateway.send(mobileNumber, message);

        logger.add("SmsNumber", mobileNumber);
        logger.add("SmsMessage", message);
        logger.add("SmsTransactionID", result);
    }
}
