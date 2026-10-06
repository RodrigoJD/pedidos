package br.com.exemplo.pedidos.controller;

import br.com.exemplo.pedidos.model.Pedido;
import br.com.exemplo.pedidos.model.RespostaPedido;
import br.com.exemplo.pedidos.model.StatusAtual;
import br.com.exemplo.pedidos.service.StatusService;
import br.com.exemplo.pedidos.messaging.PedidoPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static br.com.exemplo.pedidos.enums.Status.RECEBIDO;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    public record NovoPedido(
            @NotNull UUID id,
            @NotBlank String produto,
            @Min(1) int quantidade,
            @NotNull LocalDateTime dataCriacao
    ) {
    }

    private final PedidoPublisher pedidoPublisher;
    private final StatusService statusService;

    public PedidoController(
            PedidoPublisher pedidoPublisher,
            StatusService statusService) {

        this.pedidoPublisher = pedidoPublisher;
        this.statusService = statusService;
    }

    @PostMapping
    public ResponseEntity<RespostaPedido> criar(
            @Valid @RequestBody NovoPedido request) {

        Pedido pedido = new Pedido(
                request.id(),
                request.produto().trim(),
                request.quantidade(),
                request.dataCriacao()
        );

        statusService.atualizar(
                pedido.id(),
                RECEBIDO.name(),
                null
        );

        pedidoPublisher.publicar(pedido);

        return ResponseEntity
                .accepted()
                .body(new RespostaPedido(
                        pedido.id(),
                        RECEBIDO.name(),
                        "Pedido recebido para processamento"
                ));
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<StatusAtual> status(
            @PathVariable UUID id) {

        return statusService.buscar(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
