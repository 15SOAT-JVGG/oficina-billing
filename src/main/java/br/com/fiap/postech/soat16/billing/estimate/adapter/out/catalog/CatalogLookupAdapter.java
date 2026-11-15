package br.com.fiap.postech.soat16.billing.estimate.adapter.out.catalog;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.catalog.application.port.out.CatalogSnapshotPort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.CatalogLookupPort;
import br.com.fiap.postech.soat16.billing.estimate.application.result.CatalogItemSnapshotResult;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CatalogLookupAdapter implements CatalogLookupPort {

    private final CatalogSnapshotPort catalog;

    @Override
    public Uni<Optional<CatalogItemSnapshotResult>> findService(UUID id) {
        return catalog.findServiceSnapshot(id).map(this::toEstimateSnapshot);
    }

    @Override
    public Uni<Optional<CatalogItemSnapshotResult>> findPart(UUID id) {
        return catalog.findPartSnapshot(id).map(this::toEstimateSnapshot);
    }

    private Optional<CatalogItemSnapshotResult> toEstimateSnapshot(
            Optional<br.com.fiap.postech.soat16.billing.catalog.application.result.CatalogSnapshotResult> item) {
        return item.map(snapshot -> new CatalogItemSnapshotResult(
                snapshot.id(), snapshot.name(), snapshot.unitPrice(), snapshot.active()));
    }
}
