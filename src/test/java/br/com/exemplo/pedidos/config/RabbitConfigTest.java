package br.com.exemplo.pedidos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import static org.assertj.core.api.Assertions.assertThat;


class RabbitConfigTest {

    private RabbitConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitConfig();
        ReflectionTestUtils.setField(config, "suffix", "rodrigo");
    }

    @Test
    void deveGerarNomeDaFilaDeEntrada() {
        assertEquals("pedidos.entrada.rodrigo", config.entrada());
    }

    @Test
    void deveGerarNomeDaDlq() {
        assertEquals("pedidos.entrada.rodrigo.dlq", config.dlq());
    }

    @Test
    void deveGerarNomeDaFilaDeSucesso() {
        assertEquals("pedidos.status.sucesso.rodrigo", config.sucesso());
    }

    @Test
    void deveGerarNomeDaFilaDeFalha() {
        assertEquals("pedidos.status.falha.rodrigo", config.falha());
    }

    @Test
    void deveCriarConversorJackson() {
        assertNotNull(config.jacksonConverter());
        assertInstanceOf(Jackson2JsonMessageConverter.class, config.jacksonConverter());
    }

    @Test
    void deveCriarRabbitTemplateComConversorConfigurado() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Jackson2JsonMessageConverter converter = config.jacksonConverter();

        RabbitTemplate template = config.rabbitTemplate(connectionFactory, converter);

        assertNotNull(template);
        assertSame(converter, template.getMessageConverter());
    }

    @Test
    void deveDeclararAsFilas() {
        RabbitConfig config = new RabbitConfig();

        ReflectionTestUtils.setField(config, "suffix", "rodrigo");

        Declarables declarables = config.filas();

        List<String> queueNames = declarables.getDeclarables().stream()
                .filter(Queue.class::isInstance)
                .map(Queue.class::cast)
                .map(Queue::getName)
                .toList();

        assertThat(queueNames)
                .contains(
                        "pedidos.entrada.rodrigo",
                        "pedidos.entrada.rodrigo.dlq",
                        "pedidos.status.sucesso.rodrigo",
                        "pedidos.status.falha.rodrigo"
                );
    }
}
