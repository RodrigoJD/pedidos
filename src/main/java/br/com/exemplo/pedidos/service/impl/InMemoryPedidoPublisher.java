package br.com.exemplo.pedidos.service.impl;

import br.com.exemplo.pedidos.messaging.InMemoryPedidoQueue;
import br.com.exemplo.pedidos.model.Pedido;
import br.com.exemplo.pedidos.service.PedidoPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class InMemoryPedidoPublisher implements PedidoPublisher {
    private final InMemoryPedidoQueue queue;

    public InMemoryPedidoPublisher(InMemoryPedidoQueue queue) {
        this.queue = queue;
    }

    @Override
    public void publicar(Pedido pedido) {
        queue.publicar(pedido);
    }
}
