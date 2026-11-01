# Quality gates: build, cobertura, análise estática, SonarCloud e CI

`./mvnw verify` é o gate inteiro num comando, local e no CI: compila, roda `*Test`, roda `*IT`,
aplica Spotless, Checkstyle, PMD/CPD e SpotBugs, gera o relatório JaCoCo e reprova abaixo de 80% de
linhas. O SonarCloud roda em seguida no CI e bloqueia o PR se o quality gate falhar. Este documento é
a especificação do `pom.xml` e do workflow; `BuildContractTest`
([executable-rules.md](../architecture/executable-rules.md)) impede que o pom se afaste dela.

## Diferenças deliberadas em relação ao `oficina-app`

| No monólito | Aqui | Por quê |
|---|---|---|
| `skipITs=true`; Surefire roda `*IT` só com `-Pitest`; CI nunca passa `-Pitest` | Failsafe roda `*IT` em `integration-test` por padrão; `-DskipITs` é opt-out local | Issue #107 exige `*IT` com Postgres real no CI; cobertura passa a refletir o serviço inteiro |
| Cobertura medida só com unitários | Um `jacoco.exec` recebe Surefire e Failsafe (`append`), relatório e `check` no `verify` após o Failsafe | O gate de 80% vale sobre a soma |
| `sonar.exclusions`/`sonar.coverage.exclusions` para DTOs | Nenhuma exclusão | Não há gate fictício: o que vai para a imagem é medido |
| Sonar apontando para SonarQube de homelab, desligado | SonarCloud via `sonar-maven-plugin`, `-Dsonar.qualitygate.wait=true` | Repositório público; gate bloqueante exigido pela spec |
| Testes de arquitetura por varredura de texto | ArchUnit (`archunit-junit5`) + testes de contrato | Regras por padrão de pacote, válidas para contextos futuros |
| Dois Hibernate | Só o reativo | Ver [persistence.md](persistence.md) |
| `quarkus-smallrye-jwt-build` em produção | Só `test` | Ver [security.md](security.md) |

Tudo o mais (versões, regras de Checkstyle/PMD, limiar do SpotBugs, ordem de imports) é **idêntico**
ao monólito, para que uma pessoa ou agente transite entre os repositórios sem reaprender a régua.

## Build (Maven)

**[OBRIGATÓRIO]** `./mvnw` sempre; nunca `mvn` da máquina. Wrapper Maven 3.9.x, Java 21
(`maven.compiler.release=21`), `quarkus-bom` 3.32.3, mesmos números do monólito. Lombok 1.18.x como
`provided` + `annotationProcessorPaths`.

**[OBRIGATÓRIO]** Plugins, todos ligados à fase `verify` e todos com falha bloqueante:

| Plugin | Versão (igual ao monólito) | Configuração |
|---|---|---|
| `spotless-maven-plugin` | 3.6.0 | `importOrder` `java,javax,jakarta,org,com,br,`; `removeUnusedImports`; `trimTrailingWhitespace`; `endWithNewline`; goal `check` |
| `maven-checkstyle-plugin` | 3.6.0 | `config/checkstyle/checkstyle.xml` copiado do monólito (140 colunas, sem `*` import, chaves obrigatórias, nomes); `failsOnError`; inclui testes |
| `maven-pmd-plugin` | 3.28.0 (PMD 7.24.0) | `config/pmd/ruleset.xml` copiado do monólito (`bestpractices` + `errorprone` com as mesmas exclusões); goals `check` e `cpd-check`; `failOnViolation` |
| `spotbugs-maven-plugin` | 4.9.8.3 | `threshold=Medium`, `effort=Default`, `config/spotbugs/exclude.xml` **vazio** no início; cada exclusão futura leva comentário com a classe e o motivo |
| `jacoco-maven-plugin` | 0.8.14 | `prepare-agent`; `report` em `target/jacoco-report`; `check` com `BUNDLE` / `LINE` / `COVEREDRATIO` ≥ `${jacoco.minimum-line-coverage}` (= `0.80`) |
| `maven-surefire-plugin` | 3.5.4 | inclui `**/*Test.java`; `argLine` com `@{argLine}` + byte-buddy-agent (mesmo truque do monólito para o Mockito no JDK 21) |
| `maven-failsafe-plugin` | 3.5.4 | inclui `**/*IT.java`; goals `integration-test` e `verify`; mesmo `argLine` |
| `sonar-maven-plugin` | 5.1.x | sem configuração no pom além de `sonar.host.url=https://sonarcloud.io` e `sonar.coverage.jacoco.xmlReportPaths` |

**[OBRIGATÓRIO]** Ordem de declaração: Failsafe antes do JaCoCo, para que `report` e `check` (ambos
em `verify`) executem depois do `failsafe:verify` e vejam a cobertura dos `*IT`.

**[OBRIGATÓRIO]** Dependências de teste: `quarkus-junit5`, `quarkus-junit5-mockito`,
`quarkus-test-vertx`, `quarkus-smallrye-jwt-build`, `rest-assured`, `org.testcontainers:postgresql`,
`com.tngtech.archunit:archunit-junit5` (1.4.x), `org.yaml:snakeyaml` (versão do BOM, para o
`ConfigurationContractTest`).

**[EVITAR]** Perfil `itest`, propriedade `skipTests` em qualquer workflow, `-Dmaven.test.failure.ignore`,
`<testFailureIgnore>`, `<excludes>` no JaCoCo, baixar `jacoco.minimum-line-coverage`.

## Cobertura

**[OBRIGATÓRIO]** ≥ 80% de linhas no bundle, unitários + integração. Relatório em
`target/jacoco-report/index.html`, XML em `target/jacoco-report/jacoco.xml` (lido pelo Sonar).

**[OBRIGATÓRIO]** Cobertura abaixo do gate se resolve escrevendo o teste que falta
([testing.md](testing.md)). Exclusão, rebaixamento do limiar ou mudança de contador são
[JUSTIFICAR] com ADR — e a justificativa "para passar o build" não é aceita.

## SonarCloud

**[OBRIGATÓRIO]** Projeto no SonarCloud para `15SOAT-JVGG/oficina-billing`, com o quality gate padrão
**Sonar way** (sem gate customizado afrouxado). A análise roda no CI após o `verify`:

```shell
./mvnw --batch-mode sonar:sonar \
  -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
  -Dsonar.organization="$SONAR_ORGANIZATION" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.qualitygate.wait=true
```

`qualitygate.wait=true` faz o passo falhar quando o gate reprova; é isso que o torna bloqueante.
`SONAR_TOKEN` é secret; `SONAR_PROJECT_KEY` e `SONAR_ORGANIZATION` são variáveis do repositório. O
job **falha explicitamente** se algum dos três estiver ausente — nunca pula a análise em silêncio.

**[OBRIGATÓRIO]** Única supressão herdada: `java:S110` em `**/*Exception.java` (uma exceção de
domínio herda quatro níveis do JDK antes da hierarquia própria). Qualquer outra supressão é
[JUSTIFICAR] com comentário no pom.

**Pendente (não automatizável a partir deste repositório):** criar a organização/projeto no
SonarCloud e gravar o token e as variáveis. Ver [../agents/workflow.md](../agents/workflow.md),
seção "Limitações conhecidas".

## CI (GitHub Actions)

**[OBRIGATÓRIO]** Workflow de PR para `main`, `homolog` e `production`, sem entrada para pular testes,
com jobs:

| Job | O quê | Fonte |
|---|---|---|
| `secrets_gitleaks` | `gitleaks detect` no histórico (`fetch-depth: 0`), `.gitleaksignore` explícito | igual ao `ci.yml` do monólito |
| `sast_semgrep` | `semgrep scan --config p/ci --error`, com a mesma exclusão da regra de tag mutável de action | igual ao monólito |
| `build` | `actions/setup-java@v5` Temurin 21 com cache Maven; `./mvnw --batch-mode verify`; upload de `target/jacoco-report` como artefato | `verify` inclui `*IT` (o runner `ubuntu-latest` tem Docker para o Testcontainers) |
| `sonar` | depende de `build`; `./mvnw sonar:sonar ... -Dsonar.qualitygate.wait=true`; `fetch-depth: 0` para blame | novo |

**[OBRIGATÓRIO]** `concurrency` por ref com `cancel-in-progress: true` no CI de PR;
`permissions: contents: read`.

**[OBRIGATÓRIO]** Lint do título do PR por Conventional Commits (`amannn/action-semantic-pull-request`,
mesmos tipos do monólito: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`, `ci`,
`build`, `style`).

**[RECOMENDADO]** Reusar o `pull_request_template.md` do monólito, acrescentando a seção de
evidências de [../agents/workflow.md](../agents/workflow.md).

**Pendente (configuração do repositório no GitHub):** ruleset protegendo `main`, `homolog` e
`production` — só por PR, checks `build`, `sonar`, `secrets_gitleaks`, `sast_semgrep` obrigatórios,
sem force-push. Deploy por branch segue o ADR-0007 do monólito e é assunto da issue de deploy
(#117), não de #107.

## Como ler uma falha

| Falhou em | Onde olhar | O que não fazer |
|---|---|---|
| Spotless | `./mvnw spotless:apply` corrige formatação e ordem de import | Desligar o `check` |
| Checkstyle | Console: arquivo, linha, regra | Adicionar supressão |
| PMD/CPD | `target/pmd.xml`, `target/cpd.xml` | Excluir regra no ruleset sem justificativa |
| SpotBugs | `target/spotbugsXml.xml`; `./mvnw spotbugs:gui` para navegar | Exclusão ampla no `exclude.xml` |
| JaCoCo `check` | `target/jacoco-report/index.html`, ordenar por linhas não cobertas | Excluir pacote |
| Surefire/Failsafe | `target/surefire-reports/`, `target/failsafe-reports/` | `testFailureIgnore` |
| Testes de arquitetura | Mensagem do ArchUnit lista cada violação com classe e dependência | Mudar a regra |
| Sonar | Link do quality gate no log; issues no painel do projeto | Marcar como "won't fix" sem revisão |
