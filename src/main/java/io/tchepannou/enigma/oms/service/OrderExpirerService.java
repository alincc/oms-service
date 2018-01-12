package io.tchepannou.enigma.oms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.core.rest.RestClient;
import io.tchepannou.core.rest.RestConfig;
import io.tchepannou.core.rest.impl.DefaultRestClient;
import io.tchepannou.core.rest.impl.JsonSerializer;
import io.tchepannou.enigma.oms.client.OrderStatus;
import io.tchepannou.enigma.oms.domain.Order;
import io.tchepannou.enigma.oms.repository.OrderRepository;
import io.tchepannou.enigma.oms.support.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class OrderExpirerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderExpirerService.class);

    @Autowired
    private OrderService service;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(cron = "${enigma.service.expirer.cron}")
    public void run (){
        run(OrderStatus.NEW);
        run(OrderStatus.PENDING);
    }

    private void run(OrderStatus status){
        final Date now = DateHelper.now();
        final List<Order> orders = orderRepository.findByStatusAndExpiryDateTimeLessThan(status, now);

        final RestConfig config = new RestConfig();
        config.setSerializer(new JsonSerializer(objectMapper));
        config.setClientInfo("oms-service");
        final RestClient rest = new DefaultRestClient(config);

        LOGGER.info("{} {} orders to expire", orders.size(), status);
        for (final Order order : orders){
            try {
                service.expire(order.getId(), rest);
            } catch (Exception e){
                LOGGER.warn("Unable to expire Order#{}", order.getId());
            }
        }

    }
}
