package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.catalog.application.port.out.CatalogSnapshotPort;
import br.com.fiap.postech.soat16.billing.catalog.application.result.CatalogSnapshotResult;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CatalogSnapshotRepository implements CatalogSnapshotPort {

    private final ServiceItemRepository serviceItems;
    private final PartRepository parts;

    @Override
    public Uni<Optional<CatalogSnapshotResult>> findServiceSnapshot(UUID id) {
        return serviceItems.findServiceItemById(id).map(item -> item == null ? Optional.empty()
                : Optional.of(new CatalogSnapshotResult(item.getId(), item.getName(), item.getBasePrice(), item.isActive())));
    }

    @Override
    public Uni<Optional<CatalogSnapshotResult>> findPartSnapshot(UUID id) {
        return parts.findPartById(id).map(item -> item == null ? Optional.empty()
                : Optional.of(new CatalogSnapshotResult(item.getId(), item.getName(), item.getUnitPrice(), true)));
    }
}
