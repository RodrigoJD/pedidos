package br.com.exemplo.pedidos.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusPedido(
        UUID idPedido,
        String status,
        LocalDateTime dataProcessamento,
        String mensagemErro
) {
}
