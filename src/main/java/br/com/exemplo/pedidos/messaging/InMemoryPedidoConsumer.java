package br.com.exemplo.pedidos.messaging;

import br.com.exemplo.pedidos.exception.ProcessamentoException;
import br.com.exemplo.pedidos.model.Pedido;
import br.com.exemplo.pedidos.service.impl.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import static br.com.exemplo.pedidos.enums.Status.*;

@Component
@Profile("local")
public class InMemoryPedidoConsumer {
    private static final Logger log = LoggerFactory.getLogger(InMemoryPedidoConsumer.class);

    private final InMemoryPedidoQueue queue;
    private final StatusService statusService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LongSupplier delaySupplier;
    private final DoubleSupplier failureSupplier;

    @Autowired
    public InMemoryPedidoConsumer(InMemoryPedidoQueue queue, StatusService statusService) {
        this(queue, statusService,
                () -> ThreadLocalRandom.current().nextLong(1000, 3001),
                () -> ThreadLocalRandom.current().nextDouble());
    }

    InMemoryPedidoConsumer(InMemoryPedidoQueue queue, StatusService statusService,
                           LongSupplier delaySupplier, DoubleSupplier failureSupplier) {
        this.queue = queue;
        this.statusService = statusService;
        this.delaySupplier = delaySupplier;
        this.failureSupplier = failureSupplier;
    }

    @PostConstruct
    void iniciar() {
        executor.submit(this::consumirContinuamente);
    }

    @PreDestroy
    void finalizar() {
        executor.shutdownNow();
    }

    private void consumirContinuamente() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processar(queue.consumir());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Consumidor local finalizado");
            }
        }
    }

    void processar(Pedido pedido) {
        statusService.atualizar(pedido.id(), PROCESSANDO.name(), null);
        log.info("Iniciando processamento local do pedido {}", pedido.id());

        try {
            Thread.sleep(delaySupplier.getAsLong());

            if (failureSupplier.getAsDouble() < 0.20) {
                throw new ProcessamentoException("Falha simulada no processamento");
            }

            statusService.atualizar(pedido.id(), SUCESSO.name(), null);
            log.info("Pedido {} processado com sucesso", pedido.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            statusService.atualizar(pedido.id(), FALHA.name(), "Processamento interrompido");
        } catch (ProcessamentoException e) {
            statusService.atualizar(pedido.id(), FALHA.name(), e.getMessage());
            queue.enviarParaDlq(pedido);
            log.error("Falha no pedido {}: {}", pedido.id(), e.getMessage());
        }
    }
}
