package br.com.exemplo.pedidos.messaging.impl;

import br.com.exemplo.pedidos.config.RabbitConfig;
import br.com.exemplo.pedidos.model.Pedido;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("rabbit")
public class PedidoPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfig rabbitConfig;

    public PedidoPublisher(RabbitTemplate rabbitTemplate, RabbitConfig rabbitConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitConfig = rabbitConfig;
    }

    public void publicar(Pedido pedido) {
        rabbitTemplate.convertAndSend(rabbitConfig.entrada(), pedido);
    }
}
