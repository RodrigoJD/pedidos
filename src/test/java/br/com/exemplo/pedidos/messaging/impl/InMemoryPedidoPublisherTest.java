package br.com.exemplo.pedidos.messaging.impl;

import br.com.exemplo.pedidos.model.Pedido;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryPedidoPublisherTest {
    @Test
    void devePublicarPedidoNaFilaEmMemoria() {
        InMemoryPedidoQueue queue = new InMemoryPedidoQueue();
        InMemoryPedidoPublisher publisher = new InMemoryPedidoPublisher(queue);
        Pedido pedido = new Pedido(UUID.randomUUID(), "Notebook", 2, LocalDateTime.now());

        publisher.publicar(pedido);

        assertEquals(1, queue.tamanhoEntrada());
    }
}
