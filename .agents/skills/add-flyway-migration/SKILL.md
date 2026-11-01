---
name: add-flyway-migration
description: Criar ou alterar uma tabela do oficina-billing com migration Flyway versionada, mantendo o Hibernate em validate e a entidade JPA espelhando o DDL. Use sempre que uma *JpaEntity nascer ou mudar.
---

# Adicionar uma migration Flyway

Por que existe: no `oficina-app` o Flyway só cria o schema e o Hibernate inventa as tabelas; aqui a
migration é a única fonte do schema (ADR-0002) e qualquer divergência derruba a aplicação. As regras
estão em `docs/engineering/persistence.md`; aqui está o procedimento.

## Passos

1. **Próximo número.** `ls src/main/resources/db/migration/` — pegue o maior `V<n>` e some 1. Nunca
   reutilize número nem edite migration já mergeada.

2. **Nome.** `V<n>__<verbo>_<objeto>.sql` em snake_case minúsculo: `V1__create_catalog_tables.sql`,
   `V2__add_version_to_service_item.sql`. `MigrationConventionsTest` reprova fora do padrão.

3. **DDL explícito**, sem `CREATE SCHEMA` (o Flyway cria `oficina_billing` por `create-schemas`) e sem
   prefixo de schema nas tabelas (o `search_path`/`currentSchema` da conexão resolve):

   ```sql
   CREATE TABLE service_item (
       service_item_id            uuid         NOT NULL,
       name                       varchar(100) NOT NULL,
       description                varchar(500),
       base_price                 numeric(10,2) NOT NULL,
       estimated_duration_minutes integer,
       active                     boolean      NOT NULL,
       created_at                 timestamptz  NOT NULL,
       updated_at                 timestamptz  NOT NULL,
       created_by                 varchar(100) NOT NULL,
       updated_by                 varchar(100) NOT NULL,
       version                    bigint       NOT NULL DEFAULT 0,
       CONSTRAINT pk_service_item PRIMARY KEY (service_item_id)
   );
   ```

   Tipo, nulidade, precisão e tamanho **todos** escritos; constraints nomeadas (`pk_`, `uk_`, `fk_`,
   `ck_`). Índice só quando houver consulta que o use.

4. **Entidade espelhando o DDL.** Em `*JpaEntity`: `@Column(name, nullable, length | precision/scale)`
   idênticos; `@Version` se houver `version`; `@Enumerated(EnumType.STRING)` com `length` igual ao da
   coluna. `CamelCaseToUnderscoresNamingStrategy` já está configurada, mas declare `name` sempre que
   o nome não for a conversão óbvia.

5. **Rollback manual** (só para alteração/remoção de dado): `src/main/resources/db/rollback/U<n>__<mesmo_nome>.sql`
   com o inverso. Para `CREATE TABLE` puro, dispensável.

6. **Prove com o banco real.** Rode um `*IT` que suba o Quarkus (qualquer `*RepositoryIT` ou
   `*ControllerIT` do agregado; skill `write-postgres-it`). A subida executa a migration e o
   `validate`; se divergir, a mensagem `Schema-validation: ...` aponta coluna e tipo. Corrija a
   **migration ou a entidade**, nunca `schema-management.strategy`.

7. **Documente** se a decisão de modelagem for nova (tabela nova, tipo incomum, índice): parágrafo em
   `docs/engineering/persistence.md` ou nota no PR.

## Verificações que vão reprovar se você errar

- `MigrationConventionsTest`: nome fora do padrão, versão duplicada, `CREATE SCHEMA`, referência a
  `oficina_mecanica`.
- `ConfigurationContractTest`: alguém trocou `validate` por `update`/`drop-and-create`.
- O próprio `*IT`: entidade e migration divergentes.

## O que não fazer

- `ALTER TABLE` numa migration antiga para "arrumar": migration nova.
- `IF NOT EXISTS` em `CREATE TABLE` para esconder migration que já rodou de outro jeito.
- Dados de exemplo em migration (`INSERT` de seed): massa sintética é script de ambiente, não schema.
- `DROP` sem rollback em `db/rollback/` e sem nota no PR.
