package br.com.exemplo.pedidos.messaging;

import br.com.exemplo.pedidos.config.RabbitConfig;
import br.com.exemplo.pedidos.exception.ProcessamentoException;
import br.com.exemplo.pedidos.model.Pedido;
import br.com.exemplo.pedidos.model.StatusPedido;
import br.com.exemplo.pedidos.service.impl.StatusService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import static br.com.exemplo.pedidos.enums.Status.*;

@Component
@Profile("rabbit")
public class PedidoConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitConfig rabbitConfig;
    private final StatusService statusService;
    private final LongSupplier delaySupplier;
    private final DoubleSupplier failureSupplier;

    @Autowired
    public PedidoConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitConfig rabbitConfig,
            StatusService statusService) {
        this(
                rabbitTemplate,
                rabbitConfig,
                statusService,
                () -> ThreadLocalRandom.current().nextLong(1000, 3001),
                () -> ThreadLocalRandom.current().nextDouble()
        );
    }

    PedidoConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitConfig rabbitConfig,
            StatusService statusService,
            LongSupplier delaySupplier,
            DoubleSupplier failureSupplier) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitConfig = rabbitConfig;
        this.statusService = statusService;
        this.delaySupplier = delaySupplier;
        this.failureSupplier = failureSupplier;
    }

    @RabbitListener(queues = "#{rabbitConfig.entrada()}")
    public void consumir(
            Pedido pedido,
            Message message,
            Channel channel) throws IOException {

        long deliveryTag =
                message.getMessageProperties().getDeliveryTag();

        log.info("Iniciando processamento do pedido {}", pedido.id());

        statusService.atualizar(pedido.id(), PROCESSANDO.name(), null);

        try {
            Thread.sleep(delaySupplier.getAsLong());

            if (failureSupplier.getAsDouble() < 0.20) {
                throw new ProcessamentoException(
                        "Falha simulada no processamento"
                );
            }

            rabbitTemplate.convertAndSend(
                    rabbitConfig.sucesso(),
                    new StatusPedido(
                            pedido.id(),
                            SUCESSO.name(),
                            LocalDateTime.now(),
                            null
                    )
            );

            statusService.atualizar(pedido.id(), SUCESSO.name(), null);
            channel.basicAck(deliveryTag, false);

            log.info("Pedido {} processado com sucesso", pedido.id());

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            statusService.atualizar(
                    pedido.id(),
                    FALHA.name(),
                    "Processamento interrompido"
            );

            channel.basicNack(deliveryTag, false, true);

        } catch (ProcessamentoException exception) {
            rabbitTemplate.convertAndSend(
                    rabbitConfig.falha(),
                    new StatusPedido(
                            pedido.id(),
                            FALHA.name(),
                            LocalDateTime.now(),
                            exception.getMessage()
                    )
            );

            statusService.atualizar(
                    pedido.id(),
                    FALHA.name(),
                    exception.getMessage()
            );

            channel.basicReject(deliveryTag, false);

            log.error(
                    "Falha no pedido {}: {}",
                    pedido.id(),
                    exception.getMessage()
            );
        }
    }
}
