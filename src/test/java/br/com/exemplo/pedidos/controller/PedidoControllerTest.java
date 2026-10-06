package br.com.exemplo.pedidos.controller;

import br.com.exemplo.pedidos.model.RespostaPedido;
import br.com.exemplo.pedidos.model.StatusAtual;
import br.com.exemplo.pedidos.model.Pedido;
import br.com.exemplo.pedidos.messaging.PedidoPublisher;
import br.com.exemplo.pedidos.service.StatusService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static br.com.exemplo.pedidos.enums.Status.PROCESSANDO;
import static br.com.exemplo.pedidos.enums.Status.RECEBIDO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidoControllerTest {

    private final PedidoPublisher publisher = mock(PedidoPublisher.class);
    private final StatusService statusService = new StatusService();
    private final PedidoController controller = new PedidoController(publisher, statusService);

    @Test
    void deveCriarPedidoEPublicar() {
        UUID id = UUID.randomUUID();
        PedidoController.NovoPedido request = new PedidoController.NovoPedido(
                id, " Notebook ", 2, LocalDateTime.now()
        );

        var response = controller.criar(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().id());
        assertEquals(RECEBIDO.name(), response.getBody().status());
        assertEquals(RECEBIDO.name(), statusService.buscar(id).orElseThrow().status());
        verify(publisher).publicar(any(Pedido.class));
    }

    @Test
    void deveRetornarStatusQuandoPedidoExistir() {
        UUID id = UUID.randomUUID();
        statusService.atualizar(id, PROCESSANDO.name(), null);

        var response = controller.status(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PROCESSANDO.name(), response.getBody().status());
    }

    @Test
    void deveRetornarNotFoundQuandoPedidoNaoExistir() {
        var response = controller.status(UUID.randomUUID());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }
}
