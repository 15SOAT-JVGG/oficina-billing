package br.com.fiap.postech.soat16.billing.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import br.com.fiap.postech.soat16.billing.catalog.application.command.CreateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.DeleteServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.FindServiceItemQuery;
import br.com.fiap.postech.soat16.billing.catalog.application.command.UpdateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.application.result.ServiceItemResult;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.InvalidServiceItemException;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.ServiceItemNotFoundException;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceItemService — orquestração do catálogo de serviços")
class ServiceItemServiceTest {

    private static final UUID ID = UUID.fromString("d4a1f3a0-9b2e-4f3a-8c1d-1a2b3c4d5e6f");
    private static final BigDecimal PRICE = new BigDecimal("80.00");

    @Mock
    private ServiceCatalogPersistencePort persistence;

    private ServiceItemService service;

    private static ServiceItem persisted() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return ServiceItem.restore(ID, "Troca de oleo", "desc", PRICE, 30, true, now, now, "system", "system");
    }

    @BeforeEach
    void setUp() {
        service = new ServiceItemService(persistence);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("deve devolver um resultado por serviço persistido")
        void shouldListAll() {
            when(persistence.listAllServiceItems()).thenReturn(Uni.createFrom().item(List.of(persisted())));

            List<ServiceItemResult> results = service.listAll().await().indefinitely();

            assertEquals(1, results.size());
            assertEquals(ID, results.get(0).id());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve devolver o serviço quando existe")
        void shouldFind() {
            when(persistence.findServiceItemById(ID)).thenReturn(Uni.createFrom().item(persisted()));

            ServiceItemResult result = service.findById(new FindServiceItemQuery(ID)).await().indefinitely();

            assertEquals("Troca de oleo", result.name());
        }

        @Test
        @DisplayName("deve falhar com ServiceItemNotFoundException quando a porta emite null")
        void shouldFailWhenMissing() {
            when(persistence.findServiceItemById(ID)).thenReturn(Uni.createFrom().nullItem());

            Uni<ServiceItemResult> result = service.findById(new FindServiceItemQuery(ID));

            assertThrows(ServiceItemNotFoundException.class, () -> result.await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("deve registrar o agregado e persistir")
        void shouldCreate() {
            when(persistence.save(any(ServiceItem.class))).thenReturn(Uni.createFrom().item(persisted()));

            ServiceItemResult result = service
                    .create(new CreateServiceItemCommand("Troca de oleo", "desc", PRICE, 30, null))
                    .await().indefinitely();

            ArgumentCaptor<ServiceItem> saved = ArgumentCaptor.forClass(ServiceItem.class);
            verify(persistence).save(saved.capture());
            assertNull(saved.getValue().getId());
            assertTrue(saved.getValue().isActive());
            assertEquals(ID, result.id());
        }

        @Test
        @DisplayName("deve propagar a exceção de domínio sem persistir quando o comando é inválido")
        void shouldNotPersistInvalid() {
            CreateServiceItemCommand command = new CreateServiceItemCommand("", null, PRICE, 30, true);
            assertThrows(InvalidServiceItemException.class, () -> service.create(command));

            verify(persistence, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve carregar, aplicar os dados novos e persistir")
        void shouldUpdate() {
            ServiceItem existing = persisted();
            when(persistence.findServiceItemById(ID)).thenReturn(Uni.createFrom().item(existing));
            when(persistence.save(existing)).thenReturn(Uni.createFrom().item(existing));

            ServiceItemResult result = service
                    .update(new UpdateServiceItemCommand(ID, "Alinhamento", null, new BigDecimal("120.00"), 45, false))
                    .await().indefinitely();

            verify(persistence).save(existing);
            assertEquals("Alinhamento", result.name());
            assertEquals(new BigDecimal("120.00"), result.basePrice());
        }

        @Test
        @DisplayName("deve falhar com ServiceItemNotFoundException sem persistir quando não existe")
        void shouldFailWhenMissing() {
            when(persistence.findServiceItemById(ID)).thenReturn(Uni.createFrom().nullItem());

            Uni<ServiceItemResult> result = service
                    .update(new UpdateServiceItemCommand(ID, "Alinhamento", null, PRICE, 45, true));

            assertThrows(ServiceItemNotFoundException.class, () -> result.await().indefinitely());
            verify(persistence, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deve concluir quando a porta confirma a remoção")
        void shouldDelete() {
            when(persistence.deleteServiceItemById(ID)).thenReturn(Uni.createFrom().item(true));

            assertNull(service.delete(new DeleteServiceItemCommand(ID)).await().indefinitely());
        }

        @Test
        @DisplayName("deve falhar com ServiceItemNotFoundException quando nada foi removido")
        void shouldFailWhenMissing() {
            when(persistence.deleteServiceItemById(ID)).thenReturn(Uni.createFrom().item(false));

            Uni<Void> result = service.delete(new DeleteServiceItemCommand(ID));

            assertThrows(ServiceItemNotFoundException.class, () -> result.await().indefinitely());
        }
    }
}
