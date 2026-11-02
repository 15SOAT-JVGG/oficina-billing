package br.com.fiap.postech.soat16.billing.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.billing.catalog.application.command.CreatePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.DeletePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.FindPartQuery;
import br.com.fiap.postech.soat16.billing.catalog.application.command.UpdatePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.application.result.PartResult;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.InvalidPartException;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.PartNotFoundException;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartService — orquestração do catálogo de peças")
class PartServiceTest {

    private static final UUID ID = UUID.fromString("7f3b2c10-5d4e-4a6b-9c8d-0e1f2a3b4c5d");
    private static final BigDecimal PRICE = new BigDecimal("35.90");

    @Mock
    private PartPersistencePort persistence;

    private PartService service;

    private static Part persisted() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Part.restore(ID, "Filtro de oleo", "desc", PRICE, "UN", PartType.PART, now, now, "system", "system");
    }

    @BeforeEach
    void setUp() {
        service = new PartService(persistence);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("deve devolver um resultado por peça persistida")
        void shouldListAll() {
            when(persistence.listAllParts()).thenReturn(Uni.createFrom().item(List.of(persisted())));

            List<PartResult> results = service.listAll().await().indefinitely();

            assertEquals(1, results.size());
            assertEquals(ID, results.get(0).id());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve devolver a peça quando existe")
        void shouldFind() {
            when(persistence.findPartById(ID)).thenReturn(Uni.createFrom().item(persisted()));

            PartResult result = service.findById(new FindPartQuery(ID)).await().indefinitely();

            assertEquals("Filtro de oleo", result.name());
            assertEquals(PartType.PART, result.partType());
        }

        @Test
        @DisplayName("deve falhar com PartNotFoundException quando a porta emite null")
        void shouldFailWhenMissing() {
            when(persistence.findPartById(ID)).thenReturn(Uni.createFrom().nullItem());

            Uni<PartResult> result = service.findById(new FindPartQuery(ID));

            assertThrows(PartNotFoundException.class, () -> result.await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("deve registrar o agregado com o tipo padrão e persistir")
        void shouldCreate() {
            when(persistence.save(any(Part.class))).thenReturn(Uni.createFrom().item(persisted()));

            PartResult result = service
                    .create(new CreatePartCommand("Filtro de oleo", "desc", PRICE, "UN", null))
                    .await().indefinitely();

            ArgumentCaptor<Part> saved = ArgumentCaptor.forClass(Part.class);
            verify(persistence).save(saved.capture());
            assertNull(saved.getValue().getId());
            assertEquals(PartType.PART, saved.getValue().getPartType());
            assertEquals(ID, result.id());
        }

        @Test
        @DisplayName("deve propagar a exceção de domínio sem persistir quando o comando é inválido")
        void shouldNotPersistInvalid() {
            CreatePartCommand command = new CreatePartCommand("Filtro", null, PRICE, "", null);

            assertThrows(InvalidPartException.class, () -> service.create(command));

            verify(persistence, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve carregar, aplicar os dados novos e persistir")
        void shouldUpdate() {
            Part existing = persisted();
            when(persistence.findPartById(ID)).thenReturn(Uni.createFrom().item(existing));
            when(persistence.save(existing)).thenReturn(Uni.createFrom().item(existing));

            PartResult result = service
                    .update(new UpdatePartCommand(ID, "Oleo 5W30", null, new BigDecimal("59.90"), "L", PartType.SUPPLY))
                    .await().indefinitely();

            verify(persistence).save(existing);
            assertEquals("Oleo 5W30", result.name());
            assertEquals(PartType.SUPPLY, result.partType());
        }

        @Test
        @DisplayName("deve falhar com PartNotFoundException sem persistir quando não existe")
        void shouldFailWhenMissing() {
            when(persistence.findPartById(ID)).thenReturn(Uni.createFrom().nullItem());

            Uni<PartResult> result = service.update(new UpdatePartCommand(ID, "Oleo", null, PRICE, "L", null));

            assertThrows(PartNotFoundException.class, () -> result.await().indefinitely());
            verify(persistence, never()).save(any());
        }

        @Test
        @DisplayName("deve falhar com PartNotFoundException quando o registro some entre a leitura e a gravação")
        void shouldFailWhenRemovedConcurrently() {
            Part existing = persisted();
            when(persistence.findPartById(ID)).thenReturn(Uni.createFrom().item(existing));
            when(persistence.save(existing)).thenReturn(Uni.createFrom().nullItem());

            Uni<PartResult> result = service.update(new UpdatePartCommand(ID, "Oleo", null, PRICE, "L", null));

            assertThrows(PartNotFoundException.class, () -> result.await().indefinitely());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deve concluir quando a porta confirma a remoção")
        void shouldDelete() {
            when(persistence.deletePartById(ID)).thenReturn(Uni.createFrom().item(true));

            assertNull(service.delete(new DeletePartCommand(ID)).await().indefinitely());
        }

        @Test
        @DisplayName("deve falhar com PartNotFoundException quando nada foi removido")
        void shouldFailWhenMissing() {
            when(persistence.deletePartById(ID)).thenReturn(Uni.createFrom().item(false));

            Uni<Void> result = service.delete(new DeletePartCommand(ID));

            assertThrows(PartNotFoundException.class, () -> result.await().indefinitely());
        }
    }
}
