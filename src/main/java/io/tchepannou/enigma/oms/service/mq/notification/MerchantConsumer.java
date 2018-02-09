package io.tchepannou.enigma.oms.service.mq.notification;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.backend.profile.MerchantBackend;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.service.Mail;
import io.tchepannou.enigma.oms.service.mq.QueueNames;
import io.tchepannou.enigma.profile.client.dto.MerchantDto;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MerchantConsumer extends BaseNotificationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantConsumer.class);

    @Autowired
    private MerchantBackend merchantBackend;

    @Transactional
    @RabbitListener(queues = QueueNames.QUEUE_NOTIFICATION_MERCHANT)
    public void consume (Integer orderId){
        LOGGER.info("Consuming {}", orderId);
        try {
            final Order order = orderRepository.findOne(orderId);
            if (!shouldConsume(order)){
                LOGGER.error("Order#{} is not valid", orderId);
                return;
            }

            final Set<Integer> merchantIds = order.getLines().stream()
                    .map(l -> l.getMerchantId())
                    .collect(Collectors.toSet());

            for (Integer merchantId : merchantIds){
                try {
                    notify(order, merchantId);
                } catch (Exception e){
                    LOGGER.warn("Unable to notify Merchant#{}", merchantId);
                }
            }
        } catch (Exception e){
            LOGGER.warn("Unable to consume message: {}", orderId, e);
        }
    }

    private void notify (Order order, Integer merchantId)
            throws InvalidCarOfferTokenException, IOException, MessagingException
    {
        if (!shouldConsume(order)){
            return;
        }
        final RestClient rest = createRestClient();
        final SiteDto site = siteBackend.findById(order.getSiteId(), rest);
        final MerchantDto merchant = merchantBackend.findById(merchantId, rest);

        final OrderMailModel model = buildOrderMail(order, site, (l) -> merchantId.equals(l.getMerchantId()), rest);

        final Mail mail = buildMail(
                "Travel Confirmation - Order #" + order.getId(),
                merchant.getEmail(),
                "merchant",
                site
        );
        mail.setModel(Collections.singletonMap("model", model));

        emailService.send(mail);
    }

    private boolean shouldConsume(final Order order){
        return order != null
                && OrderStatus.CONFIRMED.equals(order.getStatus());
    }

}
