# Decisões de domínio

O que é do Billing, como o catálogo se modela e quais invariantes já são conhecidos. Nada aqui
inventa regra de negócio: cada invariante vem do código do `oficina-app` ou da spec #106. O que não
tem fonte está listado como **pendente**, e um agente não o resolve sozinho.

## Fronteira do Billing (spec #106)

| Capacidade | Dono | Status no Billing |
|---|---|---|
| Catálogo de serviços e peças com preço | **Billing** | Issue #107 |
| Orçamento e itens com snapshot de preço, token e decisão do cliente | **Billing** | Issues posteriores (#109+) |
| Cobrança e pagamento (Mercado Pago) | **Billing** | Issues posteriores (#114, #115) |
| Saldo de estoque, mínimo, estoque baixo, reserva | Execution | **Nunca aqui** |
| Ordem de Serviço, histórico, cliente, veículo, equipe, login | OS Service (`oficina-app`) | Nunca aqui |

**[OBRIGATÓRIO]** O Billing não lê o banco de outro serviço nem expõe o seu. Dados de outros
serviços chegam por evento ou por chamada à API deles, sempre atrás de uma porta de saída.

## Bounded context `catalog`

**[JUSTIFICAR — justificado aqui]** No monólito, serviços e peças são dois contextos
(`servicecatalog`, `part`). No Billing eles formam **um** contexto, `catalog`, com dois agregados.
Motivo: têm o mesmo dono (ADMIN), o mesmo ciclo (cadastro e preço), o mesmo consumidor (orçamento),
e a spec os descreve como uma capacidade. Dois contextos obrigariam o orçamento a duas portas de
lookup e dois adaptadores para a mesma pergunta ("qual o preço disto?"). Se um dia o ciclo de vida
divergir, dividir é barato: os agregados já não se referenciam.

### Agregados

| Agregado | Identidade | Estado (fonte: `oficina-app`) | O que **não** tem |
|---|---|---|---|
| `ServiceItem` | `UUID` gerado na persistência | `name`, `description`, `basePrice`, `estimatedDurationMinutes`, `active`, auditoria (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) | — |
| `Part` | `UUID` gerado na persistência | `name`, `description`, `unitPrice`, `unit`, `partType` (`PART` \| `SUPPLY`), auditoria | `stockQuantity`, `minimumStock`, `isLowStock`, `decreaseStock`, `increaseStock` |

Os dois agregados são independentes: nenhum referencia o outro.

### Invariantes conhecidos

Fonte: Bean Validation dos DTOs do monólito (`ServiceItemRequestDto`, `PartRequestDto`) e defaults
aplicados em `ServiceItemService.apply` e no construtor de `Part`. No monólito eles vivem só na
borda; **[OBRIGATÓRIO]** no Billing vivem também no agregado, que lança exceção de domínio ao ser
violado.

| Agregado | Invariante | Exceção de domínio (`ErrorType`) |
|---|---|---|
| `ServiceItem` | `name` não vazio | `InvalidServiceItemException` (`VALIDATION`) |
| `ServiceItem` | `basePrice` não nulo e > 0 | idem |
| `ServiceItem` | `estimatedDurationMinutes`, se informado, ≥ 0 | idem |
| `ServiceItem` | `active` ausente ⇒ `true` | regra de criação, não exceção |
| `Part` | `name` não vazio | `InvalidPartException` (`VALIDATION`) |
| `Part` | `unitPrice` não nulo e > 0 | idem |
| `Part` | `unit` não vazio | idem |
| `Part` | `partType` ausente ⇒ `PART` | regra de criação, não exceção |
| Ambos | Não encontrado por id | `ServiceItemNotFoundException` / `PartNotFoundException` (`NOT_FOUND`) |

Os nomes das exceções e códigos acima são a convenção esperada (`<Agregado>ErrorCode` com
`SERVICE_ITEM_NOT_FOUND`, `PART_NOT_FOUND`, `INVALID_SERVICE_ITEM`, `INVALID_PART`); o monólito já
usa `SERVICE_ITEM_NOT_FOUND` em `ServiceItemErrorCode`.

**[RECOMENDADO]** Representar preço como `BigDecimal` com escala 2 (é o que o banco guarda:
`numeric(10,2)`). Um value object `Money` só se justifica quando o orçamento passar a somar e
comparar valores; para o catálogo, `BigDecimal` validado no agregado basta. Introduzi-lo é
[JUSTIFICAR].

## Como modelar

**[OBRIGATÓRIO]** Agregado nasce por factory method nomeado pela intenção
(`ServiceItem.register(...)`, `Part.register(...)`) que valida todos os invariantes, e muda por
métodos de comportamento (`updateDetails(...)`, `changePrice(...)`, `deactivate()`), nunca por
setter. Reidratação a partir da persistência usa um factory separado e explícito
(`restore(id, ..., createdAt, updatedAt)`), como `Part.restore(...)` no monólito, que não revalida
e não gera auditoria.

**[OBRIGATÓRIO]** `equals`/`hashCode` de entidade baseados na identidade persistida: duas instâncias
sem `id` nunca são iguais; `hashCode` constante por classe (padrão `Customer`/`ServiceItem` do
monólito, com o comentário explicando o porquê).

**[OBRIGATÓRIO]** Value objects são imutáveis, validam no `of(...)`, implementam `equals` por valor.
Referência: `Document.of(...)` do monólito.

**[OBRIGATÓRIO]** Exceção de domínio estende `AppException` e recebe `ErrorCode` + `ErrorType`
([ADR-0003](../adr/0003-modelo-unico-de-erro-appexception.md)). Mensagem em inglês, orientada ao
cliente da API.

**[EVITAR]** No domínio: `@Setter`, `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`,
`@Builder`, `Uni`, anotações JPA ou Jackson, `LocalDateTime` (usar `OffsetDateTime` UTC).
`@Getter` e `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` são aceitos.

**[EVITAR]** "Serviço de domínio" para o catálogo. Não há operação que envolva dois agregados; o que
parecer serviço de domínio provavelmente é método de agregado.

**[EVITAR]** Soft delete ou `active` em `Part`. O monólito não tem; a spec não pede. `DELETE` remove.

## Pendências que um agente não decide sozinho

Levar para a issue (comentário) ou para o time antes de implementar; registrar a resposta aqui.

1. **Campos de estoque no contrato de `Part`.** O `PartRequestDto` do monólito exige `stockQuantity`
   (`@NotNull`) e a resposta expõe `stockQuantity`, `minimumStock`, `lowStock`; a spec tira o saldo do
   Billing. Recomendação: o `PartRequestDto` do Billing **não declara** esses campos (o Jackson do
   Quarkus ignora propriedades desconhecidas por padrão, então clientes antigos continuam recebendo
   `201`) e o `PartResponseDto` **não os devolve**. Confirmar.
2. **Fonte de `409` no catálogo.** O critério de aceite de #107 lista `409`, mas não existe hoje
   nenhuma regra que o produza (não há unicidade de `name`). Introduzir unicidade é regra nova e
   precisa de decisão explícita; sem ela, o `*IT` de `409` não se aplica ao catálogo e isso deve ser
   dito no PR.
3. **`createdBy`/`updatedBy`.** O monólito grava `"system"`. Preencher com o `upn` do JWT é melhoria
   barata e sem impacto de contrato, mas é mudança de comportamento: confirmar.
4. **Evento "peça cadastrada".** A spec diz que o Execution fica sabendo de peça nova por evento do
   Billing. Não é de #107 (mensageria vem depois); a modelagem de `Part` não deve antecipá-lo além de
   manter o agregado puro e o caso de uso de criação isolado.
