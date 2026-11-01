package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.mapper.ServiceItemRestMapper;
import br.com.fiap.postech.soat16.billing.catalog.application.ServiceItemService;
import br.com.fiap.postech.soat16.billing.catalog.application.result.ServiceItemResult;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.ServiceItemNotFoundException;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceItemController — mapeamento HTTP do catálogo de serviços")
class ServiceItemControllerTest {

    private static final UUID ID = UUID.fromString("d4a1f3a0-9b2e-4f3a-8c1d-1a2b3c4d5e6f");
    private static final ServiceItemRequestDto REQUEST =
            new ServiceItemRequestDto("Troca de oleo", "desc", new BigDecimal("80.00"), 30, true);
    private static final ServiceItemResult RESULT =
            new ServiceItemResult(ID, "Troca de oleo", "desc", new BigDecimal("80.00"), 30, true, OffsetDateTime.now());

    @Mock
    private ServiceItemService service;

    private ServiceItemController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceItemController(service);
    }

    @Nested
    @DisplayName("leitura")
    class Read {

        @Test
        @DisplayName("GET /admin/services deve devolver a lista mapeada")
        void shouldList() {
            when(service.listAll()).thenReturn(Uni.createFrom().item(List.of(RESULT)));

            List<ServiceItemResponseDto> response = controller.listAll().await().indefinitely();

            assertEquals(List.of(ServiceItemRestMapper.toResponse(RESULT)), response);
        }

        @Test
        @DisplayName("GET /admin/services/{id} deve devolver o serviço mapeado")
        void shouldFind() {
            when(service.findById(ServiceItemRestMapper.toQuery(ID))).thenReturn(Uni.createFrom().item(RESULT));

            ServiceItemResponseDto response = controller.findById(ID).await().indefinitely();

            assertEquals("Troca de oleo", response.name());
        }

        @Test
        @DisplayName("deve propagar a falha do serviço sem tratá-la")
        void shouldPropagateFailure() {
            when(service.findById(ServiceItemRestMapper.toQuery(ID)))
                    .thenReturn(Uni.createFrom().failure(new ServiceItemNotFoundException(ID)));

            Uni<ServiceItemResponseDto> response = controller.findById(ID);

            assertThrows(ServiceItemNotFoundException.class, () -> response.await().indefinitely());
        }
    }

    @Nested
    @DisplayName("escrita")
    class Write {

        @Test
        @DisplayName("POST /admin/services deve devolver 201 com Location e corpo")
        void shouldCreate() {
            when(service.create(ServiceItemRestMapper.toCreateCommand(REQUEST))).thenReturn(Uni.createFrom().item(RESULT));

            Response response = controller.create(REQUEST).await().indefinitely();

            assertEquals(201, response.getStatus());
            assertEquals("/admin/services/" + ID, response.getLocation().toString());
            assertEquals(ServiceItemRestMapper.toResponse(RESULT), response.getEntity());
        }

        @Test
        @DisplayName("PUT /admin/services/{id} deve devolver o serviço atualizado")
        void shouldUpdate() {
            when(service.update(ServiceItemRestMapper.toUpdateCommand(ID, REQUEST)))
                    .thenReturn(Uni.createFrom().item(RESULT));

            ServiceItemResponseDto response = controller.update(ID, REQUEST).await().indefinitely();

            assertEquals(ID, response.id());
        }

        @Test
        @DisplayName("DELETE /admin/services/{id} deve devolver 204 sem corpo")
        void shouldDelete() {
            when(service.delete(ServiceItemRestMapper.toDeleteCommand(ID))).thenReturn(Uni.createFrom().voidItem());

            Response response = controller.delete(ID).await().indefinitely();

            assertEquals(204, response.getStatus());
            assertNull(response.getEntity());
            verify(service).delete(ServiceItemRestMapper.toDeleteCommand(ID));
        }
    }
}
