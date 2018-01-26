package io.tchepannou.enigma.oms.service.notification;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.service.mail.Mail;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;

@Component
public class CustomerOrderMailer extends BaseOrderMailer {

    @Autowired
    private KVLogger kv;

    @Transactional
    public void notify (Integer orderId)
            throws InvalidCarOfferTokenException, IOException, MessagingException
    {
        final Order order = findOrder(orderId);
        final SiteDto site = siteBackend.findById(order.getSiteId());

        final OrderMailModel model = buildOrderMail(order, site);

        final Mail mail = buildMail(
                "Travel Confirmation - Order #" + order.getId(),
                order.getEmail(),
                "customer",
                site
        );
        mail.setModel(Collections.singletonMap("model", model));

        // Log
        kv.add("OrderID", order.getId());
        kv.add("OrderStatus", order.getStatus().name());
        kv.add("OrderAction", "NotifyCustomer");
        kv.add("CustomerEmail", order.getEmail());

        emailService.send(mail);
    }
}
