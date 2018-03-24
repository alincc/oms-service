package io.tchepannou.enigma.oms.service.order;

import com.google.common.base.Strings;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.enigma.ferari.client.InvalidCarOfferTokenException;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.service.Mail;
import io.tchepannou.enigma.oms.service.mq.QueueNames;
import io.tchepannou.enigma.oms.service.order.model.OrderMailModel;
import io.tchepannou.enigma.refdata.client.dto.SiteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

@Component
public class CustomerConsumer extends BaseNotificationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerConsumer.class);

    @Autowired
    private MessageSource messageSource;

    @Transactional
    @RabbitListener(queues = QueueNames.QUEUE_NOTIFICATION_CUSTOMER)
    public void consume (Integer orderId){
        LOGGER.info("Consuming {}", orderId);
        try {
            final Order order = orderRepository.findOne(orderId);
            if (!shouldConsume(order)){
                LOGGER.error("Order#{} is not valid", orderId);
                return;
            }
            notify(order);
        } catch (Exception e){
            LOGGER.warn("Unable to consume message: {}", orderId, e);
        }
    }

    private boolean shouldConsume(final Order order){
        return order != null
                && !Strings.isNullOrEmpty(order.getEmail())
                && OrderStatus.CONFIRMED.equals(order.getStatus());
    }

    private void notify (final Order order)
        throws InvalidCarOfferTokenException, IOException, MessagingException {

        final RestClient rest = createRestClient();
        final SiteDto site = siteBackend.findById(order.getSiteId(), rest);

        final Locale locale = getLocale(order, site);
        final OrderMailModel model = buildOrderMail(order, site, locale, (l) -> true, rest);
        final Mail mail = buildMail(
                messageSource.getMessage("mail.customer.subject", new Object[]{}, locale),
                order.getEmail(),
                "customer",
                locale,
                site
        );
        mail.setModel(Collections.singletonMap("model", model));

        emailService.send(mail);
    }

    private Locale getLocale(final Order order, final SiteDto site) {
        final String lang = order.getLanguageCode();
        return Strings.isNullOrEmpty(lang)
                ? new Locale(site.getLanguage().getCode())
                : new Locale(lang);
    }
}
