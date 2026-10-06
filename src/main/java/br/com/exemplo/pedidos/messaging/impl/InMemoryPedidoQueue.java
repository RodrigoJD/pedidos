package br.com.exemplo.pedidos.messaging.impl;

import br.com.exemplo.pedidos.model.Pedido;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class InMemoryPedidoQueue {
    private final BlockingQueue<Pedido> entrada = new LinkedBlockingQueue<>();
    private final BlockingQueue<Pedido> dlq = new LinkedBlockingQueue<>();

    public void publicar(Pedido pedido) {
        entrada.add(pedido);
    }

    public Pedido consumir() throws InterruptedException {
        return entrada.take();
    }

    public void enviarParaDlq(Pedido pedido) {
        dlq.add(pedido);
    }

    public int tamanhoEntrada() {
        return entrada.size();
    }

    public int tamanhoDlq() {
        return dlq.size();
    }
}
