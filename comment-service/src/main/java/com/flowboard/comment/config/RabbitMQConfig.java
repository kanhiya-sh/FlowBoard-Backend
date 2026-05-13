package com.flowboard.comment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.notification.routing-key}")
    private String notificationRoutingKey;

    @Bean
    public TopicExchange flowboardExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("flowboard.notification.queue").build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange flowboardExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(flowboardExchange)
                .with(notificationRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
