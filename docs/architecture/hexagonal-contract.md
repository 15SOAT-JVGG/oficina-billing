# Contrato da arquitetura hexagonal

O código é organizado por **bounded context** e, dentro de cada contexto, por **camada**. Este
documento é o contrato: o que vai em cada pacote, quem pode depender de quem e o que é verificado
automaticamente (ver [executable-rules.md](executable-rules.md)). Para o raciocínio de domínio, ver
[domain-decisions.md](domain-decisions.md).

## Pacotes

Raiz: `br.com.fiap.postech.soat16.billing`. Contextos atuais: `catalog` e `shared`.

```
br.com.fiap.postech.soat16.billing.<contexto>
├── domain/
│   ├── model/              # agregados, entidades, value objects, enums
│   └── exception/          # exceções de domínio (extends AppException) + <Agregado>ErrorCode
├── application/
│   ├── <Agregado>Service   # casos de uso do agregado (@ApplicationScoped)
│   ├── command/            # entrada dos casos de uso: records *Command e *Query
│   ├── result/             # saída dos casos de uso: records *Result
│   ├── mapper/             # domínio -> result, quando não couber num `Result.from(...)`
│   └── port/out/           # interfaces *Port que a aplicação exige do mundo externo
└── adapter/
    ├── in/rest/
    │   ├── openapi/        # interfaces *ControllerDocs: @Path, verbos, @APIResponse, @Tag
    │   ├── controller/     # classes *Controller que implementam a interface Docs
    │   ├── dto/request/    # records *RequestDto com Bean Validation
    │   ├── dto/response/   # records *ResponseDto
    │   ├── mapper/         # *RestMapper: DTO -> Command/Query, Result -> DTO
    │   └── exception/      # *ExceptionMapper (só no shared)
    └── out/
        ├── persistence/
        │   ├── entity/     # *JpaEntity (@Entity)
        │   ├── mapper/     # *PersistenceMapper: domínio <-> JpaEntity
        │   └── <Agregado>Repository   # PanacheRepositoryBase + implementa *PersistencePort
        └── <outro>/        # outros adaptadores de saída (lookup de outro contexto, fila, e-mail)
```

**[OBRIGATÓRIO]** Toda classe de produção vive em `<contexto>.domain..`, `<contexto>.application..`
ou `<contexto>.adapter.(in|out)..`. Não existem pacotes horizontais na raiz (`controller/`,
`service/`, `repository/`, `model/`).

**[OBRIGATÓRIO]** `shared` segue a mesma forma e contém só o que é transversal: exceções base e
`ErrorType`, `ApiErrorResponseDto` e os `ExceptionMapper`, auditoria (`AuditableEntity`,
`AuditableJpaEntity`, listener), paginação se vier a existir, filtro de correlação. `shared` não
depende de nenhum contexto.

## Direção das dependências

```
adapter.in ──► application ──► domain ◄── shared.domain
                   ▲
adapter.out ───────┘ (implementa port.out; pode usar domain)
```

| De | Pode depender de | Não pode depender de |
|---|---|---|
| `domain` | o próprio `domain`, `shared.domain`, JDK | `application`, `adapter`, qualquer framework (JPA, JAX-RS, CDI, Bean Validation, Mutiny, Jackson, Quarkus, Hibernate) |
| `application` | `domain`, `shared.domain`, `shared.application`, as próprias `port.out`, CDI (`@ApplicationScoped`, `@Inject`), Mutiny, `@WithSession`/`@WithTransaction` | `adapter`, JAX-RS, JPA/Hibernate, `PanacheRepositoryBase`, Jackson, Vert.x |
| `adapter.in` | `application` (service, command, result), `domain.model` apenas para enums expostos no DTO, `shared.adapter.in` | `adapter.out`, entidades JPA |
| `adapter.out` | `application.port.out`, `domain`, `shared.adapter.out`, framework de infraestrutura | `adapter.in`, DTOs REST |

**[OBRIGATÓRIO]** `@WithSession`/`@WithTransaction` (`io.quarkus.hibernate.reactive.panache.common`)
são a única dependência de infraestrutura permitida em `application`: elas demarcam a transação do
caso de uso. Qualquer outra classe do Panache ou do Hibernate em `application` é violação.

**[EVITAR]** Enum do domínio referenciado pelo DTO *é* aceito (o monólito faz isso com `PartType`),
mas classes de domínio com estado não atravessam a borda REST: o controller só vê `Result` e DTO.

## Portas e adaptadores

**[OBRIGATÓRIO]** Toda integração com algo externo (banco, HTTP, fila, e-mail, outro contexto) começa
por uma interface em `application/port/out` nomeada `*Port` e termina numa classe em `adapter/out`
que a implementa. O serviço recebe a porta por construtor (`@RequiredArgsConstructor` + campo
`final`).

**[OBRIGATÓRIO]** Uma porta é implementada apenas em `adapter.out`. Dublês de teste ficam em
`src/test`.

**[RECOMENDADO]** Uma porta de persistência por agregado (`ServiceCatalogPersistencePort`,
`PartPersistencePort`), com métodos nomeados pela intenção do caso de uso
(`findServiceItemById`, `save`, `deleteServiceItemById`), devolvendo `Uni<T>`. Um `Uni` que emite
`null` significa "não encontrado"; o serviço converte com `onItem().ifNull().failWith(...)`.

**[RECOMENDADO]** O repositório Panache implementa a porta diretamente
(`class ServiceItemRepository implements PanacheRepositoryBase<ServiceItemJpaEntity, UUID>, ServiceCatalogPersistencePort`),
como no monólito. Introduzir uma classe adaptadora intermediária só quando houver mais de uma fonte
de dados para a mesma porta.

### Entre contextos

**[OBRIGATÓRIO]** Um contexto nunca importa `domain` nem `adapter` de outro. Quando `A` precisa de
dados de `B`, `A` declara uma porta de lookup em `A.application.port.out` com o vocabulário de `A`,
e `A.adapter.out.<b>` a implementa chamando a aplicação de `B` (serviço ou porta de `B`) e traduzindo
o resultado. Não há ciclos entre contextos.

Hoje há um único contexto de negócio (`catalog`), então a regra está dormente; ela existe para o
orçamento, que consultará o catálogo por uma porta de preço.

## Casos de uso

**[OBRIGATÓRIO]** O `Service` é orquestrador fino: carrega o agregado pela porta, delega a decisão ao
domínio, persiste, devolve `Result`. Não contém `if` de regra de negócio; isso é do agregado.

**[OBRIGATÓRIO]** Leitura com `@WithSession`, escrita com `@WithTransaction`, sempre retornando
`Uni<T>`. Um caso de uso é uma transação; não há transação aberta no controller nem no repositório.

**[RECOMENDADO]** Um `Command` por intenção (`CreateServiceItemCommand`, `UpdateServiceItemCommand`,
`DeleteServiceItemCommand`) e um `Query` por consulta (`FindServiceItemQuery`), todos records,
construídos pelo `*RestMapper`. O `Result` é um record com `static from(<Agregado>)`.

## Adaptador REST

**[OBRIGATÓRIO]** A interface `*ControllerDocs` carrega `@Path`, verbos, `@Consumes/@Produces`,
`@SecurityRequirement(name = "bearerAuth")`, `@Tag`, `@Operation`, `@APIResponse` de cada status
que a rota pode devolver e `@Valid` nos parâmetros de corpo. A classe `*Controller` implementa a
interface, é `@ApplicationScoped`, declara `@RolesAllowed` e só mapeia DTO ↔ Command/Result.

**[OBRIGATÓRIO]** O controller não lança exceção de negócio nem monta resposta de erro: isso é dos
`ExceptionMapper` do `shared` ([ADR-0003](../adr/0003-modelo-unico-de-erro-appexception.md)).

**[OBRIGATÓRIO]** Validação sintática (`@NotBlank`, `@NotNull`, `@DecimalMin`, `@Min`) fica no
`*RequestDto` com as mensagens exatas do contrato ([rest-contracts.md](rest-contracts.md)).
Invariante de negócio fica no agregado, mesmo que pareça redundante com a validação do DTO: o
domínio não confia na borda.

## Adaptador de persistência

**[OBRIGATÓRIO]** Entidade JPA (`*JpaEntity`) e agregado são classes distintas. O
`*PersistenceMapper` faz `toDomain`, `toJpaEntity` e `copyState(domínio, entidade)` para
atualizações, exatamente como `ServiceItemPersistenceMapper` do monólito. Detalhes de transação,
schema e migration em [../engineering/persistence.md](../engineering/persistence.md).

## O que não é desta arquitetura

**[EVITAR]** Interfaces `*UseCase`/`port.in` para cada caso de uso. O monólito não as tem e nada aqui
precisa delas: o controller depende do `Service` concreto do mesmo contexto, e o teste unitário do
controller o mocka. Criar porta de entrada só quando houver um segundo adaptador de entrada
(consumidor de fila, por exemplo) para o mesmo caso de uso.

**[EVITAR]** Camada de "facade", "manager", "helper" ou "util" genérica. Se um método não pertence
nem ao agregado nem ao serviço do caso de uso, a modelagem está errada.

**[EVITAR]** MapStruct ou qualquer framework de mapeamento. Os mappers manuais são curtos e
testáveis.

**[JUSTIFICAR]** Um segundo contexto de negócio, um agregado novo ou uma porta de lookup entre
contextos exigem uma nota em [domain-decisions.md](domain-decisions.md) e, se a fronteira for
difícil de reverter, um ADR.
