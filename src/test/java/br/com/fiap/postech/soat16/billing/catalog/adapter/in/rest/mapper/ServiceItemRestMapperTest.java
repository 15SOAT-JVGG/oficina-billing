package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.billing.catalog.application.command.CreateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.UpdateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.result.ServiceItemResult;

@DisplayName("ServiceItemRestMapper — DTO ↔ Command/Result")
class ServiceItemRestMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final ServiceItemRequestDto REQUEST =
            new ServiceItemRequestDto("Troca de oleo", "desc", new BigDecimal("80.00"), 30, false);

    @Test
    @DisplayName("deve mapear o request para o comando de criação campo a campo")
    void toCreateCommand() {
        CreateServiceItemCommand command = ServiceItemRestMapper.toCreateCommand(REQUEST);

        assertEquals(new CreateServiceItemCommand("Troca de oleo", "desc", new BigDecimal("80.00"), 30, false), command);
    }

    @Test
    @DisplayName("deve mapear o request e o id para o comando de atualização")
    void toUpdateCommand() {
        UpdateServiceItemCommand command = ServiceItemRestMapper.toUpdateCommand(ID, REQUEST);

        assertEquals(
                new UpdateServiceItemCommand(ID, "Troca de oleo", "desc", new BigDecimal("80.00"), 30, false), command);
    }

    @Test
    @DisplayName("deve preservar active nulo no comando (o agregado aplica o padrão)")
    void keepsNullActive() {
        ServiceItemRequestDto request = new ServiceItemRequestDto("x", null, BigDecimal.ONE, null, null);

        assertNull(ServiceItemRestMapper.toCreateCommand(request).active());
        assertNull(ServiceItemRestMapper.toUpdateCommand(ID, request).estimatedDurationMinutes());
    }

    @Test
    @DisplayName("deve mapear id para query e comando de remoção")
    void toQueryAndDelete() {
        assertEquals(ID, ServiceItemRestMapper.toQuery(ID).id());
        assertEquals(ID, ServiceItemRestMapper.toDeleteCommand(ID).id());
    }

    @Test
    @DisplayName("deve mapear o resultado para a resposta campo a campo")
    void toResponse() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        ServiceItemResult result = new ServiceItemResult(ID, "Troca de oleo", null, new BigDecimal("80.00"), 30, true, createdAt);

        ServiceItemResponseDto response = ServiceItemRestMapper.toResponse(result);

        assertEquals(new ServiceItemResponseDto(ID, "Troca de oleo", null, new BigDecimal("80.00"), 30, true, createdAt), response);
    }
}
