package io.tchepannou.enigma.oms.service.notification;

import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.service.mail.Mail;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;

@Component
public class CustomerOrderMailer extends BaseOrderMailer {

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

        emailService.send(mail);
    }
}
