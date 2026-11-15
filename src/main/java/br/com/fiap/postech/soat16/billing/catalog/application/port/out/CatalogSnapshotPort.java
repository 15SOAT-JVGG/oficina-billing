package br.com.fiap.postech.soat16.billing.catalog.application.port.out;

import java.util.Optional;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.application.result.CatalogSnapshotResult;

import io.smallrye.mutiny.Uni;

public interface CatalogSnapshotPort {
    Uni<Optional<CatalogSnapshotResult>> findServiceSnapshot(UUID id);
    Uni<Optional<CatalogSnapshotResult>> findPartSnapshot(UUID id);
}
