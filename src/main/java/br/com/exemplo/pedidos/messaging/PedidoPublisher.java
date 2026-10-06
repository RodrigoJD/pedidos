package br.com.exemplo.pedidos.messaging;

import br.com.exemplo.pedidos.model.Pedido;

public interface PedidoPublisher {
    void publicar(Pedido pedido);
}
