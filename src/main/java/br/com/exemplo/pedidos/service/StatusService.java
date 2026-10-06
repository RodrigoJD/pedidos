package br.com.exemplo.pedidos.service;

import br.com.exemplo.pedidos.model.StatusAtual;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StatusService {

    private final ConcurrentHashMap<UUID, StatusAtual> statuses =
            new ConcurrentHashMap<>();

    public void atualizar(UUID id, String status, String erro) {
        statuses.put(
                id,
                new StatusAtual(
                        id,
                        status,
                        erro,
                        LocalDateTime.now()
                )
        );
    }

    public Optional<StatusAtual> buscar(UUID id) {
        return Optional.ofNullable(statuses.get(id));
    }
}
