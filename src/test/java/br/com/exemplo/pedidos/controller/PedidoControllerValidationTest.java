package br.com.exemplo.pedidos.controller;

import br.com.exemplo.pedidos.service.PedidoPublisher;
import br.com.exemplo.pedidos.service.impl.StatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PedidoControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PedidoController controller = new PedidoController(
                mock(PedidoPublisher.class),
                new StatusService()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void deveRetornar400QuandoProdutoEstiverVazio() throws Exception {
        String json = """
                {
                  "id": "%s",
                  "produto": "",
                  "quantidade": 1,
                  "dataCriacao": "2026-10-06T10:00:00"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("produto: valor inválido"));
    }

    @Test
    void deveRetornar400QuandoQuantidadeForZero() throws Exception {
        String json = """
                {
                  "id": "%s",
                  "produto": "Notebook",
                  "quantidade": 0,
                  "dataCriacao": "2026-10-06T10:00:00"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("quantidade: valor inválido"));
    }
}
