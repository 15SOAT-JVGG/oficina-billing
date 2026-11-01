package br.com.fiap.postech.soat16.billing.catalog.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;

import io.smallrye.mutiny.Uni;

public interface PartPersistencePort {

    Uni<List<Part>> listAllParts();

    /**
     * Emite {@code null} quando não há peça com o id informado.
     */
    Uni<Part> findPartById(UUID id);

    /**
     * Insere quando o agregado ainda não tem id; atualiza quando tem. Emite {@code null} se o id
     * informado não existir mais.
     */
    Uni<Part> save(Part part);

    /**
     * Emite {@code false} quando não havia peça com o id informado.
     */
    Uni<Boolean> deletePartById(UUID id);
}
