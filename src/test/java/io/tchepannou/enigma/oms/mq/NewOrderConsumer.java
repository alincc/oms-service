package io.tchepannou.enigma.oms.mq;

import io.tchepannou.enigma.oms.config.MQConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Getter
public class NewOrderConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewOrderConsumer.class);

    private Integer orderId;

    @RabbitListener(queues = MQConfig.QUEUE)
    public void onReceive(Integer orderId){
        LOGGER.info("Consuming {}", orderId);
        this.orderId = orderId;
    }
}
