package io.tchepannou.enigma.oms.config;

import io.tchepannou.enigma.oms.service.mq.QueueNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfiguration {
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public Binding financeBinding() {
        final Queue queue = new Queue(QueueNames.QUEUE_FINANCE, true);
        final Exchange exchange = new TopicExchange(QueueNames.EXCHANGE_FINANCE);
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(queue.getName())
                .noargs();
    }

    @Bean
    public Binding customerNotificationBinding() {
        final Queue queue = new Queue(QueueNames.QUEUE_NOTIFICATION_CUSTOMER, true);
        final Exchange exchange = new TopicExchange(QueueNames.EXCHANGE_NOTIFICATION_CUSTOMER);
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(queue.getName())
                .noargs();
    }


    @Bean
    public Binding merchantNotificationBinding() {
        final Queue queue = new Queue(QueueNames.QUEUE_NOTIFICATION_MERCHANT, true);
        final Exchange exchange = new TopicExchange(QueueNames.EXCHANGE_NOTIFICATION_MERCHANT);
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(queue.getName())
                .noargs();
    }
}
