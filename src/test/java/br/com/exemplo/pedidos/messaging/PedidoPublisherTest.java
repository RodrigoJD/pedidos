package br.com.exemplo.pedidos.messaging;

import br.com.exemplo.pedidos.config.RabbitConfig;
import br.com.exemplo.pedidos.messaging.impl.RabbitPedidoPublisher;
import br.com.exemplo.pedidos.model.Pedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RabbitConfig rabbitConfig;
    @InjectMocks private RabbitPedidoPublisher publisher;

    @Test
    void devePublicarPedidoNaFilaDeEntrada() {
        String queue = "pedidos.entrada.rodrigo";
        Pedido pedido = new Pedido(UUID.randomUUID(), "Notebook", 2, LocalDateTime.now());

        when(rabbitConfig.entrada()).thenReturn(queue);

        publisher.publicar(pedido);

        verify(rabbitConfig).entrada();
        verify(rabbitTemplate).convertAndSend(queue, pedido);
    }
}
