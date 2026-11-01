# Contratos REST do catálogo

As rotas do catálogo saem do `oficina-app` e passam a responder no Billing **sem que o cliente
perceba**: mesmos caminhos, verbos, status, papéis, DTOs e mensagens de validação. O API Gateway
encaminha `/admin/services` e `/admin/parts` para cá. Fonte: `ServiceItemControllerDocs`,
`PartControllerDocs`, DTOs, mappers e `ValidationExceptionMapper` do monólito; collection Postman
`Oficina-Mecanica-E2E`.

**[OBRIGATÓRIO]** Qualquer divergência em relação a este documento é [JUSTIFICAR] e precisa aparecer
no PR. A skill `check-contract-parity` descreve como conferir.

## Rotas

Sem prefixo `/v1` (é assim no monólito). `@SecurityRequirement(name = "bearerAuth")` em todas.

### Serviços — `@Path("/admin/services")`, tag `Service Catalog`

| Verbo | Caminho | Papéis | Sucesso | Erros |
|---|---|---|---|---|
| `GET` | `/admin/services` | ADMIN, MECHANIC | `200` array de `ServiceItemResponseDto` | `401`, `403` |
| `GET` | `/admin/services/{id}` | ADMIN, MECHANIC | `200` `ServiceItemResponseDto` | `401`, `403`, `404` |
| `POST` | `/admin/services` | ADMIN | `201` + `Location: /admin/services/{id}` + corpo | `400`, `401`, `403` |
| `PUT` | `/admin/services/{id}` | ADMIN | `200` `ServiceItemResponseDto` | `400`, `401`, `403`, `404` |
| `DELETE` | `/admin/services/{id}` | ADMIN | `204` sem corpo | `401`, `403`, `404` |

### Peças — `@Path("/admin/parts")`, tag `Parts and Supplies`

| Verbo | Caminho | Papéis | Sucesso | Erros |
|---|---|---|---|---|
| `GET` | `/admin/parts` | ADMIN, MECHANIC | `200` array de `PartResponseDto` | `401`, `403` |
| `GET` | `/admin/parts/{id}` | ADMIN, MECHANIC | `200` `PartResponseDto` | `401`, `403`, `404` |
| `POST` | `/admin/parts` | ADMIN | `201` + `Location: /admin/parts/{id}` + corpo | `400`, `401`, `403` |
| `PUT` | `/admin/parts/{id}` | ADMIN | `200` `PartResponseDto` | `400`, `401`, `403`, `404` |
| `DELETE` | `/admin/parts/{id}` | ADMIN | `204` sem corpo | `401`, `403`, `404` |

**Não existem no Billing** (são do Execution, spec #106): `GET /admin/parts/low-stock` e
`PATCH /admin/parts/{id}/stock`. Não criar stubs nem redirecionamentos.

**[OBRIGATÓRIO]** `@RolesAllowed({"ADMIN", "MECHANIC"})` na classe do controller e
`@RolesAllowed("ADMIN")` nos métodos de escrita, como no monólito. `PUT` é substituição completa.

## DTOs

Mensagens de validação em inglês, idênticas às do monólito; são parte do contrato porque aparecem no
corpo do `400`.

### `ServiceItemRequestDto`

| Campo | Tipo | Validação | Mensagem |
|---|---|---|---|
| `name` | `String` | `@NotBlank` | `Service name is required` |
| `description` | `String` | — | — |
| `basePrice` | `BigDecimal` | `@NotNull`, `@DecimalMin("0.01")` | `Base price is required` / `Base price must be greater than zero` |
| `estimatedDurationMinutes` | `Integer` | `@Min(0)` | `Estimated duration cannot be negative` |
| `active` | `Boolean` | — | ausente ⇒ `true` |

### `ServiceItemResponseDto` (`@JsonInclude(NON_NULL)`)

`id: UUID`, `name`, `description`, `basePrice: BigDecimal`, `estimatedDurationMinutes: Integer`,
`active: boolean`, `createdAt: OffsetDateTime`.

### `PartRequestDto`

| Campo | Tipo | Validação | Mensagem |
|---|---|---|---|
| `name` | `String` | `@NotBlank` | `Part name is required` |
| `description` | `String` | — | — |
| `unitPrice` | `BigDecimal` | `@NotNull`, `@DecimalMin("0.01")` | `Unit price is required` / `Price must be greater than zero` |
| `unit` | `String` | `@NotBlank` | `Unit is required` |
| `partType` | `PartType` (`PART`, `SUPPLY`) | — | ausente ⇒ `PART` |

Campos do monólito **removidos** (pendência 1 em [domain-decisions.md](domain-decisions.md)):
`stockQuantity` (era `@NotNull`, `@Min(0)`), `minimumStock` (`@Min(0)`). Clientes que ainda os enviam
não quebram: propriedades desconhecidas são ignoradas por padrão no Quarkus
(`quarkus.jackson.fail-on-unknown-properties=false`). **[OBRIGATÓRIO]** Não ativar essa propriedade.

### `PartResponseDto`

`id: UUID`, `name`, `description`, `unitPrice: BigDecimal`, `unit`, `partType: PartType`,
`createdAt`.

Campos do monólito **removidos**: `stockQuantity`, `minimumStock`, `lowStock`.

**[JUSTIFICAR]** `createdAt` das peças era `LocalDateTime` no monólito (JSON sem offset,
`2026-01-01T10:00:00`); dos serviços, `OffsetDateTime` (`2026-01-01T10:00:00Z`). O Billing padroniza
em `OffsetDateTime` UTC. É divergência de formato num campo que nenhum cliente conhecido lê; precisa
constar no PR.

## Erros

### Status por `ErrorType` ([ADR-0003](../adr/0003-modelo-unico-de-erro-appexception.md))

| `ErrorType` | Status | Quando |
|---|---|---|
| `VALIDATION` | `400` | Invariante de domínio violado (além do Bean Validation do DTO) |
| `UNAUTHORIZED` | `401` | Reservado; o `401` de token ausente/inválido é do Quarkus |
| `NOT_FOUND` | `404` | `ServiceItemNotFoundException`, `PartNotFoundException` |
| `CONFLICT` | `409` | Sem fonte no catálogo hoje (pendência 2) |
| `GONE` | `410` | Não se aplica ao catálogo |
| `BUSINESS` | `422` | Não se aplica ao catálogo |

### Corpos

**`400` de Bean Validation — [OBRIGATÓRIO] idêntico ao monólito.** Portar `ValidationExceptionMapper`
como está:

```json
{ "status": 400, "error": "Validation Error", "violations": ["basePrice: Base price is required"], "timestamp": "..." }
```

`violations` traz `campo: mensagem`, só o último segmento do caminho, em ordem alfabética.

**`404` e demais `AppException` — `ApiErrorResponseDto`:**

```json
{ "status": 404, "type": "NOT_FOUND", "code": "SERVICE_ITEM_NOT_FOUND", "message": "Service item not found: <id>", "path": "/admin/services/<id>", "timestamp": "..." }
```

**[JUSTIFICAR — justificado no ADR-0003]** No monólito o `404` do catálogo sai pelo mapper legado
(`{status, error, message, timestamp}` com `"Service not found with id: <id>"` /
`"Part/Supply not found with id: <id>"`). O Billing usa o formato estruturado que o próprio monólito
já emite para serviço não encontrado nos fluxos de OS (`ServiceItemNotFoundException`,
`SERVICE_ITEM_NOT_FOUND`). O status não muda; o corpo muda. Postman e Swagger afirmam só o status.
**Confirmar com o time antes do merge de #107** e registrar a resposta aqui.

**`401`/`403`:** resposta padrão do Quarkus (sem corpo definido pelo projeto), como no monólito. Não
criar mapper para elas.

**`500`:** mapper de fallback (`Throwable`) devolve `ApiErrorResponseDto` com `status: 500`,
`message: "Internal server error"` e `traceId`, e registra a exceção com o mesmo `traceId`. Nunca
expõe mensagem interna.

## OpenAPI

**[OBRIGATÓRIO]** Cada operação documenta, por `@APIResponse`, todo status que pode devolver,
incluindo `401` e `403` (no monólito eles entram pelo esquema de segurança; aqui devem constar
explicitamente para que o teste de contrato os veja). O documento é servido em `/q/openapi`
(`?format=json`) e o Swagger UI em `/q/swagger-ui`.

**[OBRIGATÓRIO]** Um `*IT` de contrato OpenAPI por controller (modelo:
`WorkOrderOpenApiContractIT` do monólito) confere que os status documentados batem com a tabela
acima.

## Compatibilidade

**[OBRIGATÓRIO]** Antes de alterar qualquer linha deste documento, executar a skill
`check-contract-parity` contra o `oficina-app` e anexar o resultado ao PR. Mudança de status,
caminho, verbo, papel, nome de campo ou mensagem de validação é mudança de contrato e precisa de
nota explícita no PR com o impacto em Postman, Swagger e API Gateway.
