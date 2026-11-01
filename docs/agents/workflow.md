# Protocolo de execução para agentes

Como uma issue deste repositório é implementada por um agente, do início ao PR. Os nove passos são
obrigatórios e sequenciais; o Definition of Done no fim é o que o revisor confere. O princípio que
atravessa tudo: **nenhuma verificação é declarada como aprovada sem ter sido executada nesta
sessão, e toda limitação é registrada por escrito.**

## 1. Ler o harness

Leia, nesta ordem: [`AGENTS.md`](../../AGENTS.md), [`CONTEXT.md`](../../CONTEXT.md), os documentos
de `docs/architecture/` e `docs/engineering/` que o mapa do `AGENTS.md` aponta para a sua tarefa, e os
ADRs em `docs/adr/`. Se a tarefa toca persistência, leia também
[`persistence.md`](../engineering/persistence.md) inteiro; se toca rota, [`rest-contracts.md`](../architecture/rest-contracts.md)
inteiro.

Não comece a codar com o harness lido pela metade: o custo de um padrão errado é refazer a tarefa.

## 2. Entender a issue

`gh issue view <n> --comments` ([issue-tracker.md](issue-tracker.md)). Extraia:

- os critérios de aceite, um a um, como checklist;
- o que está explicitamente fora de escopo ou é de outra issue;
- os termos de domínio usados, conferindo cada um no `CONTEXT.md`.

Se um critério contradiz um documento do harness (ex.: pede um status que
[`rest-contracts.md`](../architecture/rest-contracts.md) não prevê), **pare e registre** a
contradição num comentário da issue antes de implementar. Não escolha um lado em silêncio.

## 3. Inspecionar domínio e contratos existentes

- Leia o código do `oficina-app` que a issue referencia (classes, testes, DTOs, mappers). Para o
  catálogo, os caminhos estão em [`legacy-diagnosis.md`](../architecture/legacy-diagnosis.md).
- Confira o que [`legacy-diagnosis.md`](../architecture/legacy-diagnosis.md) manda preservar e o que
  manda **não** transportar daquele código.
- Para rotas, execute a skill `check-contract-parity` e guarde a saída.
- Liste as pendências de [`domain-decisions.md`](../architecture/domain-decisions.md) que a tarefa
  encosta; cada uma precisa de resposta registrada ou de uma decisão explícita no PR.

## 4. Planejar e explicitar decisões

Antes de escrever código, escreva (no PR em rascunho ou num comentário da issue) um plano curto:

- os arquivos que serão criados por camada (domínio → porta → serviço → adaptadores → migration →
  testes), no formato da skill `implement-hexagonal-slice`;
- as decisões que a tarefa exige, cada uma classificada ([OBRIGATÓRIO] seguido / [RECOMENDADO]
  seguido ou desviado e por quê / [JUSTIFICAR] com a justificativa);
- as seams de teste: quais `*Test` e quais `*IT`, e quais status cada `*IT` cobre.

Decisão que mereça ADR (difícil de reverter, surpreendente sem contexto, resultado de trade-off real;
ver [domain.md](domain.md)) ganha o arquivo em `docs/adr/` no mesmo PR.

## 5. Implementar incrementalmente

Uma fatia vertical por vez, de dentro para fora: agregado e seu teste → porta → serviço e seu teste
→ adaptador de persistência, migration e `*RepositoryIT` → DTOs, mapper, Docs, controller e seus
testes → `*ControllerIT`. Compile e rode o teste da fatia a cada passo (`./mvnw test -Dtest=...`).

Não antecipe a fatia seguinte nem a issue seguinte. Se perceber que precisa de algo que a issue não
pede, anote e siga sem implementar.

## 6. Testar comportamento e invariantes

Siga [`testing.md`](../engineering/testing.md): invariantes do agregado um a um; orquestração do
serviço com a porta mockada; `*IT` por rota cobrindo sucesso e cada erro da tabela de contratos;
`*IT` de contrato OpenAPI. Use a skill `write-postgres-it` para os `*IT`.

## 7. Executar as verificações

```shell
./mvnw --batch-mode verify
```

Rode o comando inteiro, sem `-DskipITs`, sem `-DskipTests`, e leia a saída até o fim: contagem de
testes do Surefire e do Failsafe, resultado do JaCoCo `check`, resultado de Spotless, Checkstyle,
PMD, SpotBugs. Se algo falhar, corrija a causa — nunca a régua
([quality-gates.md](../engineering/quality-gates.md), "Como ler uma falha").

Se o ambiente não permitir executar algo (sem Docker para os `*IT`, sem rede para o Sonar), **não
declare o passo como aprovado**: registre em "Limitações" o que não rodou, por quê, e o que rodou em
substituição (ex.: `./mvnw verify -DskipITs` passou; `*IT` dependem do CI).

## 8. Revisar as próprias alterações

Com o diff na mão (`git diff main...HEAD`), confira item a item:

**Arquitetura**
- [ ] Nenhuma classe fora de `<ctx>.domain|application|adapter.(in|out)`.
- [ ] Nenhum `if` de regra de negócio no `Service`; invariantes no agregado com exceção de domínio.
- [ ] Toda integração externa atrás de `*Port` implementada em `adapter.out`.
- [ ] Domínio sem Mutiny, JPA, Jackson, Bean Validation, Lombok de estado.
- [ ] Toda exceção lançada é `AppException`; nenhum `RuntimeException`/`IllegalArgumentException` de negócio.

**Contrato**
- [ ] Rota, verbo, status, papéis, nomes de campo e mensagens de validação conferem com `rest-contracts.md`.
- [ ] `@APIResponse` documenta todo status que a rota devolve, inclusive `401`/`403`.
- [ ] Divergência de contrato, se houver, está nomeada e justificada no PR.

**Persistência**
- [ ] Toda mudança de entidade tem migration no mesmo PR; `validate` permanece.
- [ ] Nenhuma referência a `oficina_mecanica`; nenhum `CREATE SCHEMA` em migration.

**Qualidade do código**
- [ ] Sem duplicação (mesma forma em dois lugares → extrair).
- [ ] Sem abstração sem segundo uso (interface, classe base, "helper").
- [ ] Nomes do glossário; português só em texto para pessoas.
- [ ] Nenhum código comentado, `TODO` sem issue, `System.out`, `printStackTrace`.

**Testes**
- [ ] Cada invariante novo tem teste de aceitação e de violação.
- [ ] Cada rota nova tem `*IT` com sucesso e todos os erros da tabela.
- [ ] Nenhum `*Test` com `@QuarkusTest`; nenhum `*IT` com Mockito.
- [ ] `@DisplayName` descreve comportamento.

Se a skill `code-review` do time estiver instalada, execute-a contra `main` e anexe o resultado.

## 9. Apresentar evidências

O PR (e o comentário final na issue) traz, nesta estrutura:

```markdown
## Critérios de aceite
- [x] <critério 1> — evidência: <teste/arquivo/linha de log>
- [x] <critério 2> — evidência: ...
- [ ] <critério não atendido> — motivo: ...

## Decisões
- [OBRIGATÓRIO] ...: seguido (<onde>)
- [RECOMENDADO] ...: desviado porque ... (<onde>)
- [JUSTIFICAR] ...: justificativa ... (ADR-000N / nota)

## Pendências encostadas
- domain-decisions.md #1 (campos de estoque): resolvido como ... / aguardando resposta de ...

## Verificações executadas
./mvnw --batch-mode verify
  Surefire:  N testes, 0 falhas
  Failsafe:  M testes, 0 falhas
  JaCoCo:    XX,X% de linhas (gate 80%) — target/jacoco-report/index.html
  Spotless / Checkstyle / PMD / SpotBugs: OK
check-contract-parity: <resumo: rotas conferidas, divergências>

## Limitações
- <o que não rodou, por quê, e o que o substitui>  (ou "nenhuma")
```

Números são copiados da saída real do comando, não estimados.

## Definition of Done

Uma tarefa executada por agente está pronta quando **todas** as linhas abaixo são verdadeiras:

1. Cada critério de aceite da issue está marcado com evidência verificável, ou está explicitamente
   marcado como não atendido com o motivo.
2. `./mvnw --batch-mode verify` foi executado nesta sessão, passou, e a saída está no PR — ou a
   limitação que impediu a execução está registrada com o que foi executado em substituição.
3. Os testes de arquitetura ([executable-rules.md](../architecture/executable-rules.md)) passam sem
   alteração nas regras; se uma regra mudou, há ADR ou nota explicando.
4. Cobertura ≥ 80% de linhas no bundle, sem exclusão nova.
5. Toda rota tocada tem `*ControllerIT` cobrindo sucesso e cada erro da tabela de
   [rest-contracts.md](../architecture/rest-contracts.md), e o `*OpenApiContractIT` passa.
6. Toda mudança de entidade tem migration no mesmo PR e o Hibernate segue em `validate`.
7. Toda decisão [JUSTIFICAR] tem justificativa escrita; toda pendência de
   [domain-decisions.md](../architecture/domain-decisions.md) encostada tem resposta registrada ou
   está sinalizada como bloqueio.
8. `check-contract-parity` foi executada para cada rota e o resultado está no PR.
9. O diff não contém funcionalidade de outra issue, código comentado, `TODO` sem issue, segredo,
   nem abstração sem segundo uso.
10. `CONTEXT.md` foi atualizado se um termo novo entrou; `docs/` foi atualizado se um padrão mudou.
11. Título do PR em Conventional Commits; descrição no formato da seção 9.

## Limitações conhecidas do ambiente (registrar sempre que se aplicarem)

- **Sem Docker:** `*IT` não rodam; `./mvnw verify` falha no Failsafe ou, com `-DskipITs`, no gate de
  cobertura. Registrar e deixar o CI provar.
- **SonarCloud não configurado:** enquanto a organização/projeto e `SONAR_TOKEN` não existirem, o job
  `sonar` falha por configuração ausente — isso é correto (gate não pulado), mas precisa constar.
- **Rede corporativa com interceptação TLS:** o Maven pode falhar ao baixar dependências com
  `PKIX path building failed`; use um truststore temporário fora do repositório
  (`MAVEN_OPTS=-Djavax.net.ssl.trustStore=...`). Nunca versione truststore nem desative validação TLS
  no `pom.xml`.
- **Repositório ainda não existe na organização:** criar `15SOAT-JVGG/oficina-billing` é ação humana
  (ou explicitamente autorizada); o agente trabalha localmente e registra que o push está pendente.
