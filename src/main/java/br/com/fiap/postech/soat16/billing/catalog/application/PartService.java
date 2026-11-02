package br.com.fiap.postech.soat16.billing.catalog.application;

import static java.lang.Boolean.FALSE;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.catalog.application.command.CreatePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.DeletePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.FindPartQuery;
import br.com.fiap.postech.soat16.billing.catalog.application.command.UpdatePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.application.result.PartResult;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.PartNotFoundException;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class PartService {

    private final PartPersistencePort partPersistence;

    @WithSession
    public Uni<List<PartResult>> listAll() {
        return partPersistence.listAllParts()
                .map(parts -> parts.stream().map(PartResult::from).toList());
    }

    @WithSession
    public Uni<PartResult> findById(FindPartQuery query) {
        return loadOrFail(query.id()).map(PartResult::from);
    }

    @WithTransaction
    public Uni<PartResult> create(CreatePartCommand command) {
        Part part = Part.register(
                command.name(),
                command.description(),
                command.unitPrice(),
                command.unit(),
                command.partType());
        return partPersistence.save(part).map(PartResult::from);
    }

    @WithTransaction
    public Uni<PartResult> update(UpdatePartCommand command) {
        return loadOrFail(command.id())
                .flatMap(part -> {
                    part.updateDetails(
                            command.name(),
                            command.description(),
                            command.unitPrice(),
                            command.unit(),
                            command.partType());
                    return partPersistence.save(part);
                })
                // A porta emite null se o registro sumiu entre a leitura e a gravação.
                .onItem().ifNull().failWith(() -> new PartNotFoundException(command.id()))
                .map(PartResult::from);
    }

    @WithTransaction
    public Uni<Void> delete(DeletePartCommand command) {
        return partPersistence.deletePartById(command.id())
                .flatMap(deleted -> FALSE.equals(deleted)
                        ? Uni.createFrom().failure(new PartNotFoundException(command.id()))
                        : Uni.createFrom().voidItem());
    }

    private Uni<Part> loadOrFail(UUID id) {
        return partPersistence.findPartById(id)
                .onItem().ifNull().failWith(() -> new PartNotFoundException(id));
    }
}
