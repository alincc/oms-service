package io.tchepannou.enigma.oms.service.mq.notification;

import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.service.Mail;
import io.tchepannou.enigma.oms.service.mq.QueueNames;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;

@Component
public class CustomerConsumer extends BaseNotificationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerConsumer.class);

    @Transactional
    @RabbitListener(queues = QueueNames.QUEUE_NOTIFICATION_CUSTOMER)
    public void consume (Integer orderId){
        LOGGER.info("Consuming {}", orderId);
        try {
            final Order order = orderRepository.findOne(orderId);
            if (!isValidOrder(order)){
                LOGGER.error("Order#{} is not valid", orderId);
                return;
            }
            notify(order);
        } catch (Exception e){
            LOGGER.warn("Unable to consume message: {}", orderId, e);
        }
    }

    private void notify (final Order order)
        throws InvalidCarOfferTokenException, IOException, MessagingException {

        final RestClient rest = createRestClient();
        final SiteDto site = siteBackend.findById(order.getSiteId(), rest);

        final OrderMailModel model = buildOrderMail(order, site, (l) -> true, rest);
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
