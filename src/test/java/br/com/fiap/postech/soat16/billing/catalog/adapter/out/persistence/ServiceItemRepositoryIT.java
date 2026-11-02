package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("ServiceItemRepository — mapeamento contra o schema migrado pelo Flyway")
class ServiceItemRepositoryIT {

    @Inject
    ServiceItemRepository repository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable exception) {
            throw new IllegalStateException("Falha ao preparar dados de teste", exception);
        }
    }

    private ServiceItem seed(String name) {
        return inTransaction(() -> repository.save(
                ServiceItem.register(name, "desc", new BigDecimal("80.00"), 30, true)));
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve inserir o agregado novo, gerar id e preencher a auditoria")
        void shouldInsert() {
            ServiceItem saved = seed("Alinhamento - " + UUID.randomUUID());

            assertNotNull(saved.getId());
            assertNotNull(saved.getCreatedAt());
            assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
            assertEquals("system", saved.getCreatedBy());
        }

        @Test
        @DisplayName("deve atualizar o agregado existente mantendo id e createdAt")
        void shouldUpdate() {
            ServiceItem saved = seed("Balanceamento - " + UUID.randomUUID());
            String newName = "Balanceamento atualizado - " + UUID.randomUUID();

            ServiceItem updated = inTransaction(() -> repository.findServiceItemById(saved.getId())
                    .flatMap(found -> {
                        found.updateDetails(newName, null, new BigDecimal("150.00"), 60, false);
                        return repository.save(found);
                    }));
            ServiceItem reloaded = inTransaction(() -> repository.findServiceItemById(saved.getId()));

            assertEquals(saved.getId(), updated.getId());
            assertEquals(newName, reloaded.getName());
            assertEquals(new BigDecimal("150.00"), reloaded.getBasePrice());
            assertFalse(reloaded.isActive());
            assertEquals(saved.getCreatedAt(), reloaded.getCreatedAt());
        }

        @Test
        @DisplayName("deve emitir null ao atualizar um id que não existe mais")
        void shouldEmitNullWhenUpdatingMissing() {
            ServiceItem ghost = ServiceItem.restore(
                    UUID.randomUUID(), "x", null, BigDecimal.ONE, 1, true, null, null, null, null);

            assertNull(inTransaction(() -> repository.save(ghost)));
        }

        @Test
        @DisplayName("deve recusar nome acima do tamanho da coluna")
        void shouldRejectOversizedName() {
            ServiceItem oversized = ServiceItem.register("x".repeat(101), null, BigDecimal.ONE, 1, true);

            assertThrows(IllegalStateException.class, () -> inTransaction(() -> repository.save(oversized)));
        }
    }

    @Nested
    @DisplayName("findServiceItemById / listAllServiceItems")
    class Find {

        @Test
        @DisplayName("deve encontrar pelo id e listar o persistido")
        void shouldFindAndList() {
            ServiceItem saved = seed("Revisao - " + UUID.randomUUID());

            ServiceItem found = inTransaction(() -> repository.findServiceItemById(saved.getId()));
            boolean listed = inTransaction(() -> repository.listAllServiceItems()).contains(saved);

            assertEquals(saved, found);
            assertEquals(saved.getName(), found.getName());
            assertTrue(listed);
        }

        @Test
        @DisplayName("deve emitir null para id inexistente")
        void shouldEmitNullWhenMissing() {
            assertNull(inTransaction(() -> repository.findServiceItemById(UUID.randomUUID())));
        }
    }

    @Nested
    @DisplayName("deleteServiceItemById")
    class Delete {

        @Test
        @DisplayName("deve remover e devolver true; a busca seguinte emite null")
        void shouldDelete() {
            ServiceItem saved = seed("Descartavel - " + UUID.randomUUID());

            Boolean deleted = inTransaction(() -> repository.deleteServiceItemById(saved.getId()));

            assertTrue(deleted);
            assertNull(inTransaction(() -> repository.findServiceItemById(saved.getId())));
        }

        @Test
        @DisplayName("deve devolver false para id inexistente")
        void shouldReturnFalseWhenMissing() {
            assertEquals(Boolean.FALSE, inTransaction(() -> repository.deleteServiceItemById(UUID.randomUUID())));
        }
    }
}
