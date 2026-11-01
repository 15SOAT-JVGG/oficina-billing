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

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.mapper.PartRestMapper;
import br.com.fiap.postech.soat16.billing.catalog.application.PartService;
import br.com.fiap.postech.soat16.billing.catalog.application.result.PartResult;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.PartNotFoundException;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartController — mapeamento HTTP do catálogo de peças")
class PartControllerTest {

    private static final UUID ID = UUID.fromString("7f3b2c10-5d4e-4a6b-9c8d-0e1f2a3b4c5d");
    private static final PartRequestDto REQUEST =
            new PartRequestDto("Filtro de oleo", "desc", new BigDecimal("35.90"), "UN", PartType.PART);
    private static final PartResult RESULT =
            new PartResult(ID, "Filtro de oleo", "desc", new BigDecimal("35.90"), "UN", PartType.PART, OffsetDateTime.now());

    @Mock
    private PartService service;

    private PartController controller;

    @BeforeEach
    void setUp() {
        controller = new PartController(service);
    }

    @Nested
    @DisplayName("leitura")
    class Read {

        @Test
        @DisplayName("GET /admin/parts deve devolver a lista mapeada")
        void shouldList() {
            when(service.listAll()).thenReturn(Uni.createFrom().item(List.of(RESULT)));

            List<PartResponseDto> response = controller.listAll().await().indefinitely();

            assertEquals(List.of(PartRestMapper.toResponse(RESULT)), response);
        }

        @Test
        @DisplayName("GET /admin/parts/{id} deve devolver a peça mapeada")
        void shouldFind() {
            when(service.findById(PartRestMapper.toQuery(ID))).thenReturn(Uni.createFrom().item(RESULT));

            PartResponseDto response = controller.findById(ID).await().indefinitely();

            assertEquals("Filtro de oleo", response.name());
        }

        @Test
        @DisplayName("deve propagar a falha do serviço sem tratá-la")
        void shouldPropagateFailure() {
            when(service.findById(PartRestMapper.toQuery(ID)))
                    .thenReturn(Uni.createFrom().failure(new PartNotFoundException(ID)));

            Uni<PartResponseDto> response = controller.findById(ID);

            assertThrows(PartNotFoundException.class, () -> response.await().indefinitely());
        }
    }

    @Nested
    @DisplayName("escrita")
    class Write {

        @Test
        @DisplayName("POST /admin/parts deve devolver 201 com Location e corpo")
        void shouldCreate() {
            when(service.create(PartRestMapper.toCreateCommand(REQUEST))).thenReturn(Uni.createFrom().item(RESULT));

            Response response = controller.create(REQUEST).await().indefinitely();

            assertEquals(201, response.getStatus());
            assertEquals("/admin/parts/" + ID, response.getLocation().toString());
            assertEquals(PartRestMapper.toResponse(RESULT), response.getEntity());
        }

        @Test
        @DisplayName("PUT /admin/parts/{id} deve devolver a peça atualizada")
        void shouldUpdate() {
            when(service.update(PartRestMapper.toUpdateCommand(ID, REQUEST))).thenReturn(Uni.createFrom().item(RESULT));

            PartResponseDto response = controller.update(ID, REQUEST).await().indefinitely();

            assertEquals(ID, response.id());
        }

        @Test
        @DisplayName("DELETE /admin/parts/{id} deve devolver 204 sem corpo")
        void shouldDelete() {
            when(service.delete(PartRestMapper.toDeleteCommand(ID))).thenReturn(Uni.createFrom().voidItem());

            Response response = controller.delete(ID).await().indefinitely();

            assertEquals(204, response.getStatus());
            assertNull(response.getEntity());
            verify(service).delete(PartRestMapper.toDeleteCommand(ID));
        }
    }
}
