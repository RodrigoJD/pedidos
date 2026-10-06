package br.com.exemplo.pedidos.messaging;

import br.com.exemplo.pedidos.model.Pedido;
import br.com.exemplo.pedidos.service.impl.StatusService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static br.com.exemplo.pedidos.enums.Status.FALHA;
import static br.com.exemplo.pedidos.enums.Status.SUCESSO;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryPedidoConsumerTest {
    @Test
    void deveProcessarPedidoComSucesso() {
        InMemoryPedidoQueue queue = new InMemoryPedidoQueue();
        StatusService status = new StatusService();
        InMemoryPedidoConsumer consumer = new InMemoryPedidoConsumer(queue, status, () -> 0L, () -> 0.50);
        Pedido pedido = new Pedido(UUID.randomUUID(), "Notebook", 1, LocalDateTime.now());

        consumer.processar(pedido);

        assertEquals(SUCESSO.name(), status.buscar(pedido.id()).orElseThrow().status());
        assertEquals(0, queue.tamanhoDlq());
    }
    @Test
    void deveEnviarFalhaParaDlq() {
        InMemoryPedidoQueue queue = new InMemoryPedidoQueue();
        StatusService status = new StatusService();
        InMemoryPedidoConsumer consumer = new InMemoryPedidoConsumer(queue, status, () -> 0L, () -> 0.10);
        Pedido pedido = new Pedido(UUID.randomUUID(), "Notebook", 1, LocalDateTime.now());

        consumer.processar(pedido);

        assertEquals(FALHA.name(), status.buscar(pedido.id()).orElseThrow().status());
        assertEquals(1, queue.tamanhoDlq());
    }
}
