# Persistência, transações e migrations

Hibernate Reactive + Panache sobre PostgreSQL 16, num banco próprio do Billing, com o schema
inteiramente definido por migrations Flyway. As duas decisões de fundo têm ADR:
[ADR-0001](../adr/0001-hibernate-reactive-e-mutiny-por-paridade-com-o-os.md) (reativo) e
[ADR-0002](../adr/0002-flyway-dono-do-schema-e-hibernate-validate.md) (Flyway dono do schema).

## Banco e isolamento

**[OBRIGATÓRIO]** Banco PostgreSQL **separado** do OS Service (spec #106, história 35), com usuário
próprio cujos grants se limitam ao schema `oficina_billing`. Nenhuma credencial do Billing alcança
`oficina_mecanica`, e vice-versa. Em homolog/produção o banco vem do `oficina-infra-db`; local, do
`docker compose` do próprio repositório.

**[OBRIGATÓRIO]** Schema `oficina_billing`, fixado em `quarkus.hibernate-orm.database.default-schema`,
`quarkus.flyway.schemas`, `currentSchema=` na URL JDBC e `search_path=` na URL reativa. Nenhuma
entidade declara `schema=` na anotação `@Table`: o schema é configuração, não código.

**[OBRIGATÓRIO]** Variáveis de ambiente: `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`,
`POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `POSTGRES_SSLMODE` (`disable` local, `require` no RDS),
`POSTGRES_TRUST_ALL` (só quando o certificado do servidor não está no truststore, como no RDS do
lab). Mesmos nomes do monólito para reaproveitar ConfigMap e scripts, exceto `INFRA_HOST_POSTGRES`,
que vira `POSTGRES_HOST`.

## Duas conexões, um banco

O Hibernate Reactive fala pelo cliente reativo (`quarkus-reactive-pg-client`); o Flyway só fala JDBC
(`quarkus-jdbc-postgresql`). Por isso a mesma datasource tem URL reativa e URL JDBC apontando para o
mesmo banco. É assim no monólito e continua aqui.

**[OBRIGATÓRIO]** Só `quarkus-hibernate-reactive-panache`; o `quarkus-hibernate-orm-panache`
bloqueante não entra ([BuildContractTest](../architecture/executable-rules.md)).

## Migrations (Flyway)

**[OBRIGATÓRIO]** `quarkus.flyway.migrate-at-start=true`, `schemas=oficina_billing`,
`create-schemas=true`, `locations=db/migration`. O Flyway cria o schema; a primeira migration cria
tabelas. Nenhuma migration contém `CREATE SCHEMA`.

**[OBRIGATÓRIO]** `quarkus.hibernate-orm.schema-management.strategy=validate` em **todos** os
perfis, inclusive `%test`. Se a entidade e a tabela divergem, a aplicação não sobe — e o `*IT`
também não, o que é o comportamento desejado.

**[OBRIGATÓRIO]** Nome `V<n>__descricao_em_snake_case.sql`, `n` sequencial, uma mudança lógica por
arquivo, DDL explícito (tipo, nulidade, precisão, chaves, índices). Toda migration é idempotente na
leitura (dá para entender o schema lendo só os arquivos) e **nunca é editada depois de mergeada**:
correção é migration nova.

**[RECOMENDADO]** Script de rollback manual `db/rollback/U<n>__mesma_descricao.sql` para migrations
que alteram ou removem dado; para `CREATE TABLE` puro é dispensável.

**[RECOMENDADO]** Nomes de tabela no singular (`service_item`, `part`), colunas em `snake_case`,
chave primária `<tabela>_id UUID`, preço `numeric(10,2) NOT NULL`, textos com tamanho explícito
(`name varchar(100)`, `description varchar(500)`, `unit varchar(10)`, `part_type varchar(10)`),
auditoria `created_at`/`updated_at timestamptz NOT NULL`, `created_by`/`updated_by varchar NOT NULL`.
São os tipos que o monólito materializou via Hibernate; aqui eles ficam escritos.

**[RECOMENDADO]** `version bigint` com `@Version` nas duas tabelas do catálogo (o monólito só tinha em
`parts`): atualização concorrente de preço vira falha explícita em vez de "último grava".

**[EVITAR]** `hibernate-orm.schema-management.strategy: update` ou `drop-and-create` em qualquer
perfil; `quarkus.hibernate-orm.database.generation`; `init-script-path` de DevServices;
DevServices ligado (`quarkus.devservices.enabled=false` sempre; o `*IT` usa `PostgresTestResource`).

## Entidade JPA e mapper

**[OBRIGATÓRIO]** `*JpaEntity` em `adapter.out.persistence.entity`, com `@Entity(name = "...")`,
`@Table(name = "...")`, `@Id @GeneratedValue(strategy = GenerationType.UUID)`, `@Column` com
`name`, `nullable`, `length`/`precision`/`scale` espelhando a migration. Lombok `@Getter @Setter` é
aceito aqui (é um saco de dados por natureza). Estende `AuditableJpaEntity` quando o agregado estende
`AuditableEntity`.

**[OBRIGATÓRIO]** `*PersistenceMapper` final, sem instância, com `toDomain(entity)` (via
`<Agregado>.restore(...)`), `toJpaEntity(domain)` e `copyState(domain, entity)` para atualização.
Nenhuma regra de negócio no mapper.

**[OBRIGATÓRIO]** Nunca expor `*JpaEntity` fora de `adapter.out.persistence`; a porta devolve o
agregado.

## Repositório e transações

**[OBRIGATÓRIO]** `class <Agregado>Repository implements PanacheRepositoryBase<<Agregado>JpaEntity, UUID>, <Agregado>PersistencePort`,
`@ApplicationScoped`. Os métodos da porta traduzem para o domínio. `save` decide entre `persist` (sem
`id`) e `findById` + `copyState` (com `id`), como `ServiceItemRepository` do monólito. `deleteById`
devolve `Uni<Boolean>` e o serviço traduz `false` em "não encontrado".

**[OBRIGATÓRIO]** Transação demarcada no `Service`: `@WithSession` para leitura, `@WithTransaction`
para escrita. Um caso de uso é uma transação. Nem controller nem repositório abrem transação.

**[OBRIGATÓRIO]** `Uni` que emite `null` é o idioma de "não encontrado" na porta; o serviço converte
com `.onItem().ifNull().failWith(() -> new <Agregado>NotFoundException(id))`.

**[EVITAR]** Consultas HQL com string concatenada; `list("campo = " + valor)`. Usar parâmetros
nomeados do Panache (`find("name", name)`).

**[EVITAR]** `@Transactional` do JTA (é do mundo bloqueante) e `Panache.withTransaction` dentro de
`Service` (é para testes e código fora de bean CDI).

## Auditoria

**[OBRIGATÓRIO]** Portar `AuditableEntity` (domínio, `OffsetDateTime createdAt/updatedAt`,
`String createdBy/updatedBy`), `AuditableJpaEntity` (`@MappedSuperclass` com `@EntityListeners`),
`AuditJpaEntityListener` (`@PrePersist`/`@PreUpdate`, UTC) e `AuditPersistenceMapper` do monólito
para `shared`.

**[JUSTIFICAR]** O listener do monólito grava `"system"`. Preencher com o `upn` do JWT exige
acesso à `SecurityIdentity` no listener (ou passar o autor pelo `Command`); decidir com o time
(pendência 3 em [domain-decisions.md](../architecture/domain-decisions.md)).

## Ambiente local

**[RECOMENDADO]** `docker compose up` sobe Postgres e aplicação; `docker compose up -d postgres` +
`./mvnw quarkus:dev` para desenvolvimento. O Compose cria o banco `oficina_billing` com usuário
`billing`; nenhum valor sensível fica no arquivo além dos defaults de desenvolvimento, e `.env` é
opcional. A chave pública do OS para o ambiente local fica em `.local-jwt/publicKey.pem`
(ignorada pelo Git) — ver [security.md](security.md).
