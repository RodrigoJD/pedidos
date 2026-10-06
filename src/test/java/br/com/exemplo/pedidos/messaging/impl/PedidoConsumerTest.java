package br.com.exemplo.pedidos.messaging.impl;

import br.com.exemplo.pedidos.config.RabbitConfig;
import br.com.exemplo.pedidos.model.Pedido;
import br.com.exemplo.pedidos.model.StatusPedido;
import br.com.exemplo.pedidos.service.StatusService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static br.com.exemplo.pedidos.enums.Status.FALHA;
import static br.com.exemplo.pedidos.enums.Status.SUCESSO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoConsumerTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitConfig rabbitConfig = mock(RabbitConfig.class);
    private final StatusService statusService = new StatusService();
    private final Channel channel = mock(Channel.class);

    @AfterEach
    void limparInterrupcaoDaThread() {
        Thread.interrupted();
    }

    @Test
    void deveProcessarPedidoComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        Pedido pedido = pedido(id);
        PedidoConsumer consumer = consumer(0L, 0.50);
        Message message = message(42L);

        when(rabbitConfig.sucesso()).thenReturn("fila.sucesso");

        consumer.consumir(pedido, message, channel);

        assertEquals(SUCESSO.name(), statusService.buscar(id).orElseThrow().status());
        verify(rabbitTemplate).convertAndSend(eq("fila.sucesso"), any(StatusPedido.class));
        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    void deveEnviarParaFalhaERejeitarMensagemQuandoProcessamentoFalhar() throws Exception {
        UUID id = UUID.randomUUID();
        Pedido pedido = pedido(id);
        PedidoConsumer consumer = consumer(0L, 0.10);
        Message message = message(43L);

        when(rabbitConfig.falha()).thenReturn("fila.falha");

        consumer.consumir(pedido, message, channel);

        StatusPedido status = capturarStatusPublicado();

        assertEquals(id, status.idPedido());
        assertEquals(FALHA.name(), status.status());
        assertEquals("Falha simulada no processamento", status.mensagemErro());
        assertEquals(FALHA.name(), statusService.buscar(id).orElseThrow().status());
        verify(channel).basicReject(43L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void deveReenfileirarQuandoProcessamentoForInterrompido() throws Exception {
        UUID id = UUID.randomUUID();
        PedidoConsumer consumer = consumer(1000L, 0.50);
        Message message = message(44L);

        Thread.currentThread().interrupt();

        consumer.consumir(pedido(id), message, channel);

        assertEquals(
                FALHA.name(),
                statusService.buscar(id).orElseThrow().status()
        );

        assertEquals(
                "Processamento interrompido",
                statusService.buscar(id).orElseThrow().mensagemErro()
        );

        verify(channel).basicNack(44L, false, true);
        verifyNoInteractions(rabbitTemplate);
    }

    private PedidoConsumer consumer(long delay, double random) {
        return new PedidoConsumer(
                rabbitTemplate,
                rabbitConfig,
                statusService,
                () -> delay,
                () -> random
        );
    }

    private Message message(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }

    private Pedido pedido(UUID id) {
        return new Pedido(id, "Notebook", 2, LocalDateTime.now());
    }

    private StatusPedido capturarStatusPublicado() {
        ArgumentCaptor<StatusPedido> captor = ArgumentCaptor.forClass(StatusPedido.class);
        verify(rabbitTemplate).convertAndSend(eq("fila.falha"), captor.capture());
        return captor.getValue();
    }
}
