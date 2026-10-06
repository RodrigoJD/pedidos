package br.com.exemplo.pedidos.model;

import java.util.UUID;

public record RespostaPedido(
        UUID id,
        String status,
        String mensagem
) {
}
