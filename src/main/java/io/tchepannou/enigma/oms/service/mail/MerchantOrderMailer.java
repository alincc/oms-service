package io.tchepannou.enigma.oms.service.mail;

import io.tchepannou.core.logger.KVLogger;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.service.Mail;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;

@Component
public class MerchantOrderMailer extends BaseOrderMailer {

    @Autowired
    private KVLogger kv;

    @Autowired
    private MerchantBackend merchantBackend;

    @Transactional
    public void notify (Integer orderId, Integer merchantId)
            throws InvalidCarOfferTokenException, IOException, MessagingException
    {
        final Order order = findOrder(orderId);
        final SiteDto site = siteBackend.findById(order.getSiteId());
        final MerchantDto merchant = merchantBackend.findById(merchantId);

        final OrderMailModel model = buildOrderMail(order, site, (l) -> merchantId.equals(l.getMerchantId()));

        final Mail mail = buildMail(
                "Travel Confirmation - Order #" + order.getId(),
                merchant.getEmail(),
                "merchant",
                site
        );
        mail.setModel(Collections.singletonMap("model", model));

        // Log
        kv.add("OrderID", order.getId());
        kv.add("OrderStatus", order.getStatus().name());
        kv.add("Action", "NotifyMerchant");
        kv.add("Email", merchant.getEmail());

        emailService.send(mail);
    }
}
