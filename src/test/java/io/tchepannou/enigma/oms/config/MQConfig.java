package io.tchepannou.enigma.oms.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {
    public static final String QUEUE = "order-test";

    @Autowired
    private Exchange newOrderFanout;

    @Autowired
    private AmqpAdmin admin;

    @Bean
    public Binding testBinding(){
        // Given
        final Queue queue = new Queue(QUEUE, false);
        if (admin.getQueueProperties(QUEUE) == null){
            admin.declareQueue(queue);
        }
        return BindingBuilder
                .bind(queue)
                .to(newOrderFanout)
                .with(queue.getName())
                .noargs();
    }

}
