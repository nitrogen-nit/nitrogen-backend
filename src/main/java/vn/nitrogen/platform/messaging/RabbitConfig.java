package vn.nitrogen.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ dùng chung.
 *
 * <p>Chỉ khai báo converter ở bước này. Exchange/queue/binding (§17.1) và retry
 * policy (§17.2) khai báo trong PR messaging, không hardcode ở đây.
 *
 * <p>TODO: khai báo topic exchange, queue theo module, DLQ và retry policy.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }
}
