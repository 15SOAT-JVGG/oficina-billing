# Hibernate Reactive e Mutiny, por paridade com o OS Service

O Billing usa Hibernate Reactive + Panache e `Uni<T>` nas camadas de aplicação e adaptadores, como
o `oficina-app`, embora um serviço CRUD de catálogo fosse mais simples com Hibernate ORM bloqueante
e `@Transactional`. A paridade pesou mais que a simplicidade: as classes do catálogo são portadas
quase um a um do monólito, as referências que os agentes consultam (`WorkOrderControllerIT`,
`PostgresTestResource`, `@WithSession`/`@WithTransaction`, `VertxContextSupport` nos testes) são
as mesmas, e os dois serviços Postgres da Fase 4 passam a compartilhar um único idioma de
persistência, o que reduz o custo de alguém transitar entre eles. O domínio continua síncrono; a
reatividade para na aplicação.

## Considered Options

**Hibernate ORM bloqueante com Panache.** Menos conceitos (`@Transactional`, sem contexto Vert.x
nos testes), mesmo Flyway, mesma JDBC. Rejeitado porque criaria dois idiomas de persistência na
mesma organização e invalidaria o monólito como referência de código para o Billing; o ganho de
simplicidade é real mas pequeno num CRUD, e o orçamento e a cobrança (próximas issues) herdam do
monólito fluxos já escritos em Mutiny.

## Consequences

Reavaliar se a primeira implementação (#107) mostrar que a reatividade está custando mais do que a
paridade devolve — a troca é possível enquanto só o catálogo existir, e fica cara depois do
orçamento. O `quarkus-jdbc-postgresql` permanece no classpath exclusivamente para o Flyway.
