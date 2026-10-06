package br.com.exemplo.pedidos.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Configuration
@Profile("rabbit")
public class RabbitConfig {

    @Value("${app.pedidos.suffix}")
    private String suffix;

    public String entrada() {
        return "pedidos.entrada." + suffix;
    }

    public String dlq() {
        return entrada() + ".dlq";
    }

    public String sucesso() {
        return "pedidos.status.sucesso." + suffix;
    }

    public String falha() {
        return "pedidos.status.falha." + suffix;
    }

    @Bean
    Jackson2JsonMessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    Declarables filas() {

        Queue deadLetterQueue = new Queue(dlq(), true);

        Queue entrada = new Queue(
                entrada(),
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", dlq()
                )
        );

        Queue sucesso = new Queue(sucesso(), true);
        Queue falha = new Queue(falha(), true);

        return new Declarables(
                entrada,
                deadLetterQueue,
                sucesso,
                falha
        );
    }
}
