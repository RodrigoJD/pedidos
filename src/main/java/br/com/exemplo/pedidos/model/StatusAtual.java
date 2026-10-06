package br.com.exemplo.pedidos.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusAtual(
        UUID id,
        String status,
        String mensagemErro,
        LocalDateTime atualizadoEm
) {
}
