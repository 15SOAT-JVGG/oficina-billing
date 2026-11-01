package br.com.fiap.postech.soat16.billing.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.catalog.domain.exception.InvalidPartException;
import br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType;

@DisplayName("Part — invariantes da peça do catálogo")
class PartTest {

    private static final String NAME = "Filtro de oleo";
    private static final BigDecimal PRICE = new BigDecimal("35.90");
    private static final String UNIT = "UN";

    private static Part registered() {
        return Part.register(NAME, "Filtro de oleo do motor", PRICE, UNIT, PartType.PART);
    }

    private static Part restored(UUID id) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Part.restore(id, NAME, null, PRICE, UNIT, PartType.PART, now, now, "system", "system");
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("deve registrar a peça sem identidade e com os dados informados")
        void shouldRegister() {
            Part part = registered();

            assertNull(part.getId());
            assertEquals(NAME, part.getName());
            assertEquals(PRICE, part.getUnitPrice());
            assertEquals(UNIT, part.getUnit());
            assertEquals(PartType.PART, part.getPartType());
            assertNull(part.getCreatedAt());
        }

        @Test
        @DisplayName("deve classificar como PART quando partType não é informado")
        void shouldDefaultToPart() {
            assertEquals(PartType.PART, Part.register(NAME, null, PRICE, UNIT, null).getPartType());
        }

        @Test
        @DisplayName("deve aceitar insumo explícito")
        void shouldAcceptSupply() {
            assertEquals(PartType.SUPPLY, Part.register("Oleo 5W30", null, PRICE, "L", PartType.SUPPLY).getPartType());
        }

        @Test
        @DisplayName("deve recusar nome ausente")
        void shouldRejectMissingName() {
            assertInvalid(() -> Part.register(null, null, PRICE, UNIT, null), "Part name is required");
        }

        @Test
        @DisplayName("deve recusar nome em branco")
        void shouldRejectBlankName() {
            assertInvalid(() -> Part.register(" ", null, PRICE, UNIT, null), "Part name is required");
        }

        @Test
        @DisplayName("deve recusar preço ausente")
        void shouldRejectMissingPrice() {
            assertInvalid(() -> Part.register(NAME, null, null, UNIT, null), "Unit price is required");
        }

        @Test
        @DisplayName("deve recusar preço zero")
        void shouldRejectZeroPrice() {
            assertInvalid(() -> Part.register(NAME, null, BigDecimal.ZERO, UNIT, null),
                    "Price must be greater than zero");
        }

        @Test
        @DisplayName("deve recusar preço negativo")
        void shouldRejectNegativePrice() {
            assertInvalid(() -> Part.register(NAME, null, new BigDecimal("-0.01"), UNIT, null),
                    "Price must be greater than zero");
        }

        @Test
        @DisplayName("deve recusar unidade ausente")
        void shouldRejectMissingUnit() {
            assertInvalid(() -> Part.register(NAME, null, PRICE, null, null), "Unit is required");
        }

        @Test
        @DisplayName("deve recusar unidade em branco")
        void shouldRejectBlankUnit() {
            assertInvalid(() -> Part.register(NAME, null, PRICE, "  ", null), "Unit is required");
        }
    }

    @Nested
    @DisplayName("updateDetails")
    class UpdateDetails {

        @Test
        @DisplayName("deve substituir todos os dados mantendo a identidade")
        void shouldReplaceAllDetails() {
            UUID id = UUID.randomUUID();
            Part part = restored(id);

            part.updateDetails("Oleo 5W30", "Sintetico", new BigDecimal("59.90"), "L", PartType.SUPPLY);

            assertEquals(id, part.getId());
            assertEquals("Oleo 5W30", part.getName());
            assertEquals("Sintetico", part.getDescription());
            assertEquals(new BigDecimal("59.90"), part.getUnitPrice());
            assertEquals("L", part.getUnit());
            assertEquals(PartType.SUPPLY, part.getPartType());
        }

        @Test
        @DisplayName("deve voltar a PART quando partType não é informado")
        void shouldDefaultToPartOnUpdate() {
            Part part = Part.register(NAME, null, PRICE, UNIT, PartType.SUPPLY);

            part.updateDetails(NAME, null, PRICE, UNIT, null);

            assertEquals(PartType.PART, part.getPartType());
        }

        @Test
        @DisplayName("deve recusar a atualização inválida sem alterar o estado")
        void shouldRejectInvalidUpdateWithoutChangingState() {
            Part part = registered();

            assertInvalid(() -> part.updateDetails("Novo", null, PRICE, "", null), "Unit is required");

            assertEquals(NAME, part.getName());
            assertEquals(UNIT, part.getUnit());
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

            Part part = Part.restore(id, "", null, BigDecimal.ZERO, "", null, createdAt, createdAt, "system", "system");

            assertEquals(id, part.getId());
            assertEquals(createdAt, part.getCreatedAt());
            assertEquals("system", part.getUpdatedBy());
            assertNull(part.getPartType());
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
            Part part = restored(UUID.randomUUID());

            assertNotEquals(part, restored(UUID.randomUUID()));
            assertNotEquals(part, "texto");
            assertEquals(part, part);
        }
    }

    private static void assertInvalid(Runnable action, String expectedMessage) {
        InvalidPartException exception = assertThrows(InvalidPartException.class, action::run);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals("INVALID_PART", exception.getCode());
        assertEquals(ErrorType.VALIDATION, exception.getType());
    }
}
