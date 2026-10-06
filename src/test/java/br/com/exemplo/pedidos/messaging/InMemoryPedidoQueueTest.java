package br.com.exemplo.pedidos.messaging;

import br.com.exemplo.pedidos.model.Pedido;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InMemoryPedidoQueueTest {
    @Test
    void devePublicarEConsumirPedido() throws Exception {
        InMemoryPedidoQueue queue = new InMemoryPedidoQueue();
        Pedido pedido = new Pedido(UUID.randomUUID(), "Mouse", 1, LocalDateTime.now());

        queue.publicar(pedido);

        assertSame(pedido, queue.consumir());
        assertEquals(0, queue.tamanhoEntrada());
    }

    @Test
    void deveEnviarPedidoParaDlq() {
        InMemoryPedidoQueue queue = new InMemoryPedidoQueue();
        Pedido pedido = new Pedido(UUID.randomUUID(), "Mouse", 1, LocalDateTime.now());

        queue.enviarParaDlq(pedido);

        assertEquals(1, queue.tamanhoDlq());
    }
}
