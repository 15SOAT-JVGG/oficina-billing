package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;
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
@DisplayName("PartRepository — mapeamento contra o schema migrado pelo Flyway")
class PartRepositoryIT {

    @Inject
    PartRepository repository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable exception) {
            throw new IllegalStateException("Falha ao preparar dados de teste", exception);
        }
    }

    private Part seed(String name) {
        return inTransaction(() -> repository.save(
                Part.register(name, "desc", new BigDecimal("35.90"), "UN", PartType.PART)));
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve inserir o agregado novo, gerar id e preencher a auditoria")
        void shouldInsert() {
            Part saved = seed("Filtro - " + UUID.randomUUID());

            assertNotNull(saved.getId());
            assertNotNull(saved.getCreatedAt());
            assertEquals("system", saved.getCreatedBy());
            assertEquals(PartType.PART, saved.getPartType());
        }

        @Test
        @DisplayName("deve atualizar o agregado existente mantendo id e createdAt")
        void shouldUpdate() {
            Part saved = seed("Oleo - " + UUID.randomUUID());
            String newName = "Oleo 5W30 - " + UUID.randomUUID();

            Part updated = inTransaction(() -> repository.findPartById(saved.getId())
                    .flatMap(found -> {
                        found.updateDetails(newName, "Sintetico", new BigDecimal("59.90"), "L", PartType.SUPPLY);
                        return repository.save(found);
                    }));
            Part reloaded = inTransaction(() -> repository.findPartById(saved.getId()));

            assertEquals(saved.getId(), updated.getId());
            assertEquals(newName, reloaded.getName());
            assertEquals(new BigDecimal("59.90"), reloaded.getUnitPrice());
            assertEquals("L", reloaded.getUnit());
            assertEquals(PartType.SUPPLY, reloaded.getPartType());
            assertEquals(saved.getCreatedAt(), reloaded.getCreatedAt());
        }

        @Test
        @DisplayName("deve emitir null ao atualizar um id que não existe mais")
        void shouldEmitNullWhenUpdatingMissing() {
            Part ghost = Part.restore(UUID.randomUUID(), "x", null, BigDecimal.ONE, "UN", PartType.PART, null, null, null, null);

            assertNull(inTransaction(() -> repository.save(ghost)));
        }

        @Test
        @DisplayName("deve recusar unidade acima do tamanho da coluna")
        void shouldRejectOversizedUnit() {
            Part oversized = Part.register("Filtro", null, BigDecimal.ONE, "UNIDADE_LONGA", PartType.PART);

            assertThrows(IllegalStateException.class, () -> inTransaction(() -> repository.save(oversized)));
        }
    }

    @Nested
    @DisplayName("findPartById / listAllParts")
    class Find {

        @Test
        @DisplayName("deve encontrar pelo id e listar a persistida")
        void shouldFindAndList() {
            Part saved = seed("Pastilha - " + UUID.randomUUID());

            Part found = inTransaction(() -> repository.findPartById(saved.getId()));
            boolean listed = inTransaction(() -> repository.listAllParts()).contains(saved);

            assertEquals(saved, found);
            assertEquals(saved.getName(), found.getName());
            assertTrue(listed);
        }

        @Test
        @DisplayName("deve emitir null para id inexistente")
        void shouldEmitNullWhenMissing() {
            assertNull(inTransaction(() -> repository.findPartById(UUID.randomUUID())));
        }
    }

    @Nested
    @DisplayName("deletePartById")
    class Delete {

        @Test
        @DisplayName("deve remover e devolver true; a busca seguinte emite null")
        void shouldDelete() {
            Part saved = seed("Descartavel - " + UUID.randomUUID());

            Boolean deleted = inTransaction(() -> repository.deletePartById(saved.getId()));

            assertTrue(deleted);
            assertNull(inTransaction(() -> repository.findPartById(saved.getId())));
        }

        @Test
        @DisplayName("deve devolver false para id inexistente")
        void shouldReturnFalseWhenMissing() {
            assertEquals(Boolean.FALSE, inTransaction(() -> repository.deletePartById(UUID.randomUUID())));
        }
    }
}
