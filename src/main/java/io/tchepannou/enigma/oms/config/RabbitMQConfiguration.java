package io.tchepannou.enigma.oms.config;

import io.tchepannou.enigma.oms.service.mq.QueueNames;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class RabbitMQConfiguration {
    @Value("${amqp.url:amqp://guest:guest@127.0.0.1}")
    private String amqpUrl;

    @Bean
    public ConnectionFactory connectionFactory() {
        final URI rabbitMqUrl;
        try {
            rabbitMqUrl = new URI(amqpUrl);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        final CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setUsername(rabbitMqUrl.getUserInfo().split(":")[0]);
        factory.setPassword(rabbitMqUrl.getUserInfo().split(":")[1]);
        factory.setHost(rabbitMqUrl.getHost());
        factory.setPort(rabbitMqUrl.getPort());
        if (!rabbitMqUrl.getPath().isEmpty()) {
            factory.setVirtualHost(rabbitMqUrl.getPath().substring(1));
        }

        return factory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public Exchange newOrderFanoutExchange(){
        return new FanoutExchange(QueueNames.EXCHANGE_NEW_ORDER);
    }

    @Bean
    public Binding financeBinding() {
        return createBinding(QueueNames.QUEUE_FINANCE, newOrderFanoutExchange());
    }

    @Bean
    public Binding customerNotificationBinding() {
        return createBinding(QueueNames.QUEUE_NOTIFICATION_CUSTOMER, newOrderFanoutExchange());
    }

    @Bean
    public Binding merchantNotificationBinding() {
        return createBinding(QueueNames.QUEUE_NOTIFICATION_MERCHANT, newOrderFanoutExchange());
    }

    private Binding createBinding(final String name, final Exchange exchange){
        final Queue queue = new Queue(name, false);

        if (!isQueueExist(name)) {
            amqpAdmin().declareQueue(queue);
        }
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(queue.getName())
                .noargs();
    }

    private boolean isQueueExist(final String name){
        return amqpAdmin().getQueueProperties(name) != null;
    }
}
