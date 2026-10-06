package vn.nitrogen.platform.messaging;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ dùng chung.
 *
 * <p>Converter được khai báo tại đây. Exchange, queue và binding dùng chung được
 * quản lý tập trung tại {@link RabbitTopologyConfig}.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("vn.nitrogen.platform.messaging");
    }
}
