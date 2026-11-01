# O Flyway é o único dono do schema; o Hibernate apenas valida

Toda tabela, coluna, constraint e índice do banco do Billing nasce numa migration versionada em
`src/main/resources/db/migration`, e `quarkus.hibernate-orm.schema-management.strategy` é `validate`
em todos os perfis, inclusive o de teste. No `oficina-app` a configuração aparenta o mesmo (Flyway
ligado), mas `V1__create_schema.sql` cria apenas o schema e as tabelas vêm de
`schema-management: update` — o schema de produção é um efeito colateral do mapeamento JPA, e o
perfil de teste usa `drop-and-create`, de modo que os testes nunca exercitam o banco real. Aqui o
que está no repositório é o que está no banco, e um `*IT` que sobe é a prova de que entidade e
migration concordam.

## Consequences

Toda mudança de entidade exige migration no mesmo PR, senão a aplicação (e os `*IT`) não sobem. É
o custo pretendido. O Flyway continua rodando na inicialização do pod (`migrate-at-start`), como no
monólito, e por isso o `rollout status` do deploy é quem espera a migração.
