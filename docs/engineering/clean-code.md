# Clean Code e SOLID

O que torna o código deste repositório legível para a próxima pessoa ou agente. Formatação é do
Spotless e do Checkstyle (ver [quality-gates.md](quality-gates.md)); aqui ficam as decisões que a
ferramenta não toma por você.

## Nomes

**[OBRIGATÓRIO]** Vocabulário do negócio em inglês, conforme [`CONTEXT.md`](../../CONTEXT.md):
`ServiceItem`, `Part`, `basePrice`, `unitPrice`, `partType`. Nada de `item`, `product`, `price`
genéricos quando o glossário tem o termo exato.

**[OBRIGATÓRIO]** Nome revela intenção; o nome completo vence a abreviação (`estimatedDurationMinutes`,
não `estDur`). Booleanos como afirmação (`isActive`, `hasDescription`). Métodos pela ação de negócio
(`register`, `changePrice`, `deactivate`, `findServiceItemById`).

**[OBRIGATÓRIO]** Sufixo pela camada ([hexagonal-contract.md](../architecture/hexagonal-contract.md)):
`*Service`, `*Command`, `*Query`, `*Result`, `*Port`, `*Controller`, `*ControllerDocs`,
`*RequestDto`, `*ResponseDto`, `*RestMapper`, `*JpaEntity`, `*PersistenceMapper`, `*Repository`,
`*Adapter`, `*Exception`, `*ErrorCode`.

**[OBRIGATÓRIO]** Português só em texto para pessoas: `@DisplayName`, commits, PRs, documentação,
comentários. Mensagens de erro da API em inglês (são contrato).

**[EVITAR]** Constantes com nome em um idioma e valor em outro (`PECA_INSUMO = "Part/Supply"` no
monólito). Prefixos de tipo (`IServiceCatalogPort`, `AbstractX` quando há um só filho).

## Métodos e classes

**[OBRIGATÓRIO]** Um método, um nível de abstração. Guardas retornam cedo. Um método de caso de uso
lê como a história do caso de uso: carregar, decidir, persistir, devolver.

**[OBRIGATÓRIO]** Responsabilidade única por classe, verificável pela pergunta "por que esta classe
mudaria?": o agregado muda por regra de negócio; o `Service` por orquestração; o DTO por contrato; a
entidade JPA por schema.

**[RECOMENDADO]** Classes curtas. Um `Service` com mais de ~150 linhas ou mais de 7 métodos públicos
é sinal de que há dois agregados ou dois casos de uso misturados.

**[RECOMENDADO]** Parâmetros: até três; acima disso, um record (`Command`) ou um método no agregado
que receba o próprio comando.

**[EVITAR]** Flags booleanas como parâmetro para mudar comportamento; dois métodos nomeados são mais
claros.

## Literais e constantes

**[OBRIGATÓRIO]** Literal repetido vira constante nomeada (`SERVICES_PATH = "/admin/services"`).
Números com significado também (`PRICE_SCALE = 2`).

**[RECOMENDADO]** Constantes privadas na classe que as usa; `public static final` só quando outra
classe legitimamente precisa do mesmo valor (um `path` compartilhado por controller e teste).

## Comentários

**[OBRIGATÓRIO]** Comentário explica **por quê**, nunca **o quê**. Referência: o comentário sobre
identidade em `equals` do `ServiceItem` do monólito, e os comentários de decisão nos workflows do CI.
Um comentário que parafraseia o código é removido.

**[OBRIGATÓRIO]** Nenhum código comentado, `TODO` sem issue ou `FIXME` entra na `main`.

## Erros

**[OBRIGATÓRIO]** Exceções de domínio específicas, derivadas de `AppException`
([ADR-0003](../adr/0003-modelo-unico-de-erro-appexception.md)). Nunca `RuntimeException`,
`IllegalArgumentException` ou `IllegalStateException` como erro de negócio: eles viram `500` ou
`400` sem código, dependendo do mapper, e o cliente não distingue.

**[OBRIGATÓRIO]** Nunca capturar e engolir. Capturar para traduzir (infra → domínio) ou para registrar
e relançar com causa.

**[EVITAR]** Retornar `null` de método público fora do idioma Mutiny da porta de persistência
(`Uni` que emite `null` para "não encontrado"). Em código síncrono, `Optional` ou exceção.

## Lombok

**[OBRIGATÓRIO]** Permitido em adaptadores e aplicação: `@RequiredArgsConstructor` para injeção,
`@Getter`/`@Setter` em `*JpaEntity`, `@Slf4j`-equivalente não (usar `org.jboss.logging.Logger`
como o monólito).

**[OBRIGATÓRIO]** No domínio: só `@Getter` e `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`.
Nada que gere setter, construtor público ou builder
([domain-decisions.md](../architecture/domain-decisions.md)).

## SOLID na prática deste repositório

| Princípio | Onde se vê | Como se viola |
|---|---|---|
| **S**RP | Agregado decide; `Service` orquestra; mapper traduz; controller expõe | `if` de regra de negócio no `Service`; mapper que calcula; controller que monta erro |
| **O**CP | `ErrorType → status` em um único `switch` no `AppExceptionMapper`; novo erro = nova exceção + código, sem tocar o mapper | `switch` por tipo de exceção espalhado em controllers |
| **L**SP | Toda `AppException` é tratável pelo mesmo mapper | Exceção de domínio que não é `AppException` |
| **I**SP | Uma porta por necessidade (`*PersistencePort` do agregado; `*LookupPort` para outro contexto) | Porta "repositório genérico" com 15 métodos dos quais o serviço usa 3 |
| **D**IP | `Service` depende de `*Port`; `Repository` implementa a porta; a direção é verificada por ArchUnit | `Service` injetando `PanacheRepositoryBase` ou a classe concreta do repositório |

**[EVITAR]** Abstração sem segundo uso. Uma interface com uma única implementação se justifica
apenas quando é porta (fronteira do hexágono) ou quando o teste precisa dela. Fora disso, é
*speculative generality*: a classe concreta basta.

**[EVITAR]** Herança para reaproveitar código. `AuditableEntity`/`AuditableJpaEntity` são a exceção
herdada do monólito (e justificada: é estado comum a toda entidade, não comportamento).

## Reatividade (Mutiny)

**[OBRIGATÓRIO]** `Uni<T>` nas camadas `application` e `adapter`; domínio síncrono. Encadear com
`map`/`flatMap`/`onItem().ifNull().failWith(...)`/`replaceWith`; nunca `await()` em código de
produção.

**[RECOMENDADO]** Um pipeline por método; se precisar de mais de três `flatMap`, extraia métodos
privados nomeados pelo passo (`loadOrFail`, `applyChanges`, `persist`).

**[EVITAR]** Bloquear o event loop (`Thread.sleep`, I/O síncrono, `await().indefinitely()` fora de
teste).
