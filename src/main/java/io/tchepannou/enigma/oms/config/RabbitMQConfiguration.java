package io.tchepannou.enigma.oms.config;

import io.tchepannou.enigma.oms.service.QueueNames;
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

    @Bean(name=QueueNames.EXCHANGE_ORDER_CONFIRMED)
    public Exchange orderConfirmedExchange(){
        return new FanoutExchange(QueueNames.EXCHANGE_ORDER_CONFIRMED);
    }

    @Bean(name=QueueNames.EXCHANGE_ORDER_CANCELLED)
    public Exchange orderCancelledExchange() {
        return new FanoutExchange(QueueNames.EXCHANGE_ORDER_CANCELLED);
    }

    @Bean
    public Binding ticketSmsBinding() {
        return createBinding(QueueNames.QUEUE_TICKET_SMS, orderConfirmedExchange());
    }

    @Bean
    public Binding refundBinding() {
        return createBinding(QueueNames.QUEUE_ORDER_REFUND, orderCancelledExchange());
    }

    @Bean
    public Binding bookingCancelledBinding() {
        return createBinding(QueueNames.QUEUE_BOOKING_CANCEL, orderCancelledExchange());
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
