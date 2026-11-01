package br.com.fiap.postech.soat16.billing.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.catalog.domain.exception.InvalidServiceItemException;
import br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType;

@DisplayName("ServiceItem — invariantes do serviço do catálogo")
class ServiceItemTest {

    private static final String NAME = "Troca de oleo";
    private static final BigDecimal PRICE = new BigDecimal("80.00");

    private static ServiceItem registered() {
        return ServiceItem.register(NAME, "Troca de oleo e filtro", PRICE, 30, true);
    }

    private static ServiceItem restored(UUID id) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return ServiceItem.restore(id, NAME, null, PRICE, 30, true, now, now, "system", "system");
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("deve registrar o serviço sem identidade e com os dados informados")
        void shouldRegister() {
            ServiceItem serviceItem = registered();

            assertNull(serviceItem.getId());
            assertEquals(NAME, serviceItem.getName());
            assertEquals(PRICE, serviceItem.getBasePrice());
            assertEquals(30, serviceItem.getEstimatedDurationMinutes());
            assertTrue(serviceItem.isActive());
            assertNull(serviceItem.getCreatedAt());
        }

        @Test
        @DisplayName("deve considerar ativo quando active não é informado")
        void shouldDefaultToActive() {
            assertTrue(ServiceItem.register(NAME, null, PRICE, null, null).isActive());
        }

        @Test
        @DisplayName("deve aceitar inativo explícito e duração ausente")
        void shouldAcceptInactiveWithoutDuration() {
            ServiceItem serviceItem = ServiceItem.register(NAME, null, PRICE, null, false);

            assertFalse(serviceItem.isActive());
            assertNull(serviceItem.getEstimatedDurationMinutes());
        }

        @Test
        @DisplayName("deve aceitar duração zero")
        void shouldAcceptZeroDuration() {
            assertEquals(0, ServiceItem.register(NAME, null, PRICE, 0, true).getEstimatedDurationMinutes());
        }

        @Test
        @DisplayName("deve recusar nome ausente")
        void shouldRejectMissingName() {
            assertInvalid(() -> ServiceItem.register(null, null, PRICE, 30, true), "Service name is required");
        }

        @Test
        @DisplayName("deve recusar nome em branco")
        void shouldRejectBlankName() {
            assertInvalid(() -> ServiceItem.register("   ", null, PRICE, 30, true), "Service name is required");
        }

        @Test
        @DisplayName("deve recusar preço ausente")
        void shouldRejectMissingPrice() {
            assertInvalid(() -> ServiceItem.register(NAME, null, null, 30, true), "Base price is required");
        }

        @Test
        @DisplayName("deve recusar preço zero")
        void shouldRejectZeroPrice() {
            assertInvalid(() -> ServiceItem.register(NAME, null, BigDecimal.ZERO, 30, true),
                    "Base price must be greater than zero");
        }

        @Test
        @DisplayName("deve recusar preço negativo")
        void shouldRejectNegativePrice() {
            assertInvalid(() -> ServiceItem.register(NAME, null, new BigDecimal("-1.00"), 30, true),
                    "Base price must be greater than zero");
        }

        @Test
        @DisplayName("deve recusar duração negativa")
        void shouldRejectNegativeDuration() {
            assertInvalid(() -> ServiceItem.register(NAME, null, PRICE, -1, true),
                    "Estimated duration cannot be negative");
        }
    }

    @Nested
    @DisplayName("updateDetails")
    class UpdateDetails {

        @Test
        @DisplayName("deve substituir todos os dados mantendo a identidade")
        void shouldReplaceAllDetails() {
            UUID id = UUID.randomUUID();
            ServiceItem serviceItem = restored(id);

            serviceItem.updateDetails("Alinhamento", "Alinhamento e balanceamento", new BigDecimal("120.00"), 45, false);

            assertEquals(id, serviceItem.getId());
            assertEquals("Alinhamento", serviceItem.getName());
            assertEquals("Alinhamento e balanceamento", serviceItem.getDescription());
            assertEquals(new BigDecimal("120.00"), serviceItem.getBasePrice());
            assertEquals(45, serviceItem.getEstimatedDurationMinutes());
            assertFalse(serviceItem.isActive());
        }

        @Test
        @DisplayName("deve voltar a ativo quando active não é informado")
        void shouldDefaultToActiveOnUpdate() {
            ServiceItem serviceItem = ServiceItem.register(NAME, null, PRICE, 30, false);

            serviceItem.updateDetails(NAME, null, PRICE, 30, null);

            assertTrue(serviceItem.isActive());
        }

        @Test
        @DisplayName("deve recusar a atualização inválida sem alterar o estado")
        void shouldRejectInvalidUpdateWithoutChangingState() {
            ServiceItem serviceItem = registered();

            assertInvalid(() -> serviceItem.updateDetails("Novo", null, BigDecimal.ZERO, 30, true),
                    "Base price must be greater than zero");

            assertEquals(NAME, serviceItem.getName());
            assertEquals(PRICE, serviceItem.getBasePrice());
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("deve reidratar identidade e auditoria sem revalidar")
        void shouldRestoreWithoutValidation() {
            UUID id = UUID.randomUUID();
            OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");

            ServiceItem serviceItem = ServiceItem.restore(
                    id, "", null, BigDecimal.ZERO, -5, false, createdAt, createdAt, "system", "system");

            assertEquals(id, serviceItem.getId());
            assertEquals(createdAt, serviceItem.getCreatedAt());
            assertEquals("system", serviceItem.getCreatedBy());
        }
    }

    @Nested
    @DisplayName("identidade")
    class Identity {

        @Test
        @DisplayName("duas instâncias sem id nunca são iguais")
        void transientInstancesAreNeverEqual() {
            assertNotEquals(registered(), registered());
        }

        @Test
        @DisplayName("duas instâncias com o mesmo id são iguais e têm o mesmo hash")
        void sameIdMeansEqual() {
            UUID id = UUID.randomUUID();

            assertEquals(restored(id), restored(id));
            assertEquals(restored(id).hashCode(), restored(id).hashCode());
        }

        @Test
        @DisplayName("ids diferentes, instâncias diferentes; e nunca igual a outro tipo")
        void differentIdsOrTypesAreNotEqual() {
            ServiceItem serviceItem = restored(UUID.randomUUID());

            assertNotEquals(serviceItem, restored(UUID.randomUUID()));
            assertNotEquals(serviceItem, "texto");
            assertEquals(serviceItem, serviceItem);
        }
    }

    private static void assertInvalid(Runnable action, String expectedMessage) {
        InvalidServiceItemException exception = assertThrows(InvalidServiceItemException.class, action::run);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals("INVALID_SERVICE_ITEM", exception.getCode());
        assertEquals(ErrorType.VALIDATION, exception.getType());
    }
}
