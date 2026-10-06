package vn.nitrogen.platform.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    Declarables nitrogenTopology() {
        TopicExchange eventExchange = ExchangeBuilder
                .topicExchange(MessagingTopology.EVENT_EXCHANGE)
                .durable(true)
                .build();

        TopicExchange retryExchange = ExchangeBuilder
                .topicExchange(MessagingTopology.RETRY_EXCHANGE)
                .durable(true)
                .build();

        TopicExchange deadLetterExchange = ExchangeBuilder
                .topicExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();

        Queue retryQueue = QueueBuilder
                .durable(MessagingTopology.RETRY_30S_QUEUE)
                .quorum()
                .ttl(30_000)
                .deadLetterExchange(MessagingTopology.EVENT_EXCHANGE)
                .build();

        Queue deadLetterQueue = QueueBuilder
                .durable(MessagingTopology.DEAD_LETTER_QUEUE)
                .quorum()
                .build();

        return new Declarables(
                eventExchange,
                retryExchange,
                deadLetterExchange,
                retryQueue,
                deadLetterQueue,
                BindingBuilder.bind(retryQueue)
                        .to(retryExchange)
                        .with(MessagingTopology.ALL_ROUTING_KEYS),
                BindingBuilder.bind(deadLetterQueue)
                        .to(deadLetterExchange)
                        .with(MessagingTopology.ALL_ROUTING_KEYS));
    }
}
