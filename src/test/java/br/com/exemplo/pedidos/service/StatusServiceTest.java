package br.com.exemplo.pedidos.service;

import br.com.exemplo.pedidos.model.StatusAtual;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static br.com.exemplo.pedidos.enums.Status.PROCESSANDO;
import static org.junit.jupiter.api.Assertions.*;

class StatusServiceTest {

    private final StatusService service = new StatusService();

    @Test
    void deveAtualizarEBuscarStatus() {
        UUID id = UUID.randomUUID();

        service.atualizar(id, PROCESSANDO.name(), null);

        StatusAtual status = service.buscar(id).orElseThrow();

        assertEquals(id, status.id());
        assertEquals(PROCESSANDO.name(), status.status());
        assertNull(status.mensagemErro());
        assertNotNull(status.atualizadoEm());
    }

    @Test
    void deveRetornarVazioQuandoPedidoNaoExiste() {
        assertTrue(service.buscar(UUID.randomUUID()).isEmpty());
    }
}
