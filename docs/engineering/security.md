# Segurança: JWT RS256 e RBAC

O Billing **não autentica ninguém**. Ele valida tokens emitidos por outros (o OS Service para
ADMIN/MECHANIC; a Lambda para o cliente) usando a chave pública RS256 do par do OS e o mesmo issuer
(spec #106, histórias 52 e "Segurança"). Isso simplifica o monólito: não há login, hash de senha,
seed de usuário, chave privada nem `JwtKeyStartupGuard` aqui.

## Validação do token

**[OBRIGATÓRIO]** `quarkus-smallrye-jwt` + `quarkus-security`, com:

```yaml
mp:
  jwt:
    verify:
      issuer: ${JWT_ISSUER:oficina-api}
      publickey:
        location: ${JWT_PUBLIC_KEY_LOCATION:.local-jwt/publicKey.pem}
```

`JWT_ISSUER` e `JWT_PUBLIC_KEY_LOCATION` são os mesmos nomes de variável do monólito. Em
Kubernetes a chave pública chega por Secret montado (ex.: `file:/etc/jwt/publicKey.pem`); no Compose,
por volume; em dev, por `.local-jwt/publicKey.pem` copiada do par do OS.

**[OBRIGATÓRIO]** Papéis vêm da claim `groups` (`ADMIN`, `MECHANIC`), como o OS emite. Não inventar
claim nem papel novo.

**[OBRIGATÓRIO]** O Billing **nunca** assina token. Consequências verificadas por teste
([executable-rules.md](../architecture/executable-rules.md)):

- não existe `smallrye.jwt.sign.*` nem `smallrye.jwt.new-token.*` em `application.yaml`;
- `quarkus-smallrye-jwt-build` só em `<scope>test</scope>`, para os `*IT` cunharem tokens;
- nenhum arquivo com marcador PEM de chave privada no repositório; nenhum marcador de chave (privada
  ou pública) em `src/main/resources`;
- Dockerfile sem `genpkey` e sem `.pem`.

## Autorização

**[OBRIGATÓRIO]** `quarkus.security.jaxrs.deny-unannotated-endpoints=true`: toda operação tem
`@RolesAllowed` ou, se for pública por decisão explícita (nenhuma no catálogo), `@PermitAll` com
comentário justificando.

**[OBRIGATÓRIO]** No catálogo: `@RolesAllowed({"ADMIN", "MECHANIC"})` na classe do controller e
`@RolesAllowed("ADMIN")` em `create`, `update`, `delete` ([rest-contracts.md](../architecture/rest-contracts.md)).

**[OBRIGATÓRIO]** `401` (sem token, token inválido, issuer errado, expirado) e `403` (papel
insuficiente) são respondidos pelo Quarkus; não criar `ExceptionMapper` para eles nem corpo próprio.

**[OBRIGATÓRIO]** Todo `*ControllerIT` prova `401` sem token e `403` com token de MECHANIC nas
rotas de escrita ([testing.md](testing.md)).

## Testes

`JwtKeyPairTestResource` gera o par efêmero e faz o teste representar o OS Service:
`smallrye.jwt.sign.key.location` (privada, só o teste) e `mp.jwt.verify.publickey.location`
(pública, a aplicação). Token:

```java
Jwt.issuer("oficina-api").upn("integration-test").groups(Set.of("ADMIN")).expiresIn(Duration.ofHours(1)).sign();
```

**[RECOMENDADO]** Um `SecurityIT` por serviço (modelo: `SecurityIT` do monólito) com: `401` sem
token em rota administrativa, `401` com issuer diferente, `403` MECHANIC em escrita, `200` MECHANIC
em leitura, `200` ADMIN em escrita.

## Segredos e postura

**[OBRIGATÓRIO]** Nenhum segredo versionado: senha de banco, token do Sonar e qualquer credencial
chegam por variável de ambiente ou Secret. `.env` está no `.gitignore`. Gitleaks roda no histórico a
cada PR ([quality-gates.md](quality-gates.md)).

**[OBRIGATÓRIO]** Container non-root (`USER 185` na imagem; `runAsNonRoot`, `allowPrivilegeEscalation:
false`, `capabilities.drop: [ALL]` no manifesto), como no monólito.

**[RECOMENDADO]** `SWAGGER_UI_ENABLED=false` em produção; `quarkus.smallrye-openapi.management.enabled=false`
para manter o OpenAPI na 8080 (publicado pelo gateway) e health/métricas na 9000 (não publicados).

**[EVITAR]** `quarkus.http.auth.permission.*` em configuração para substituir `@RolesAllowed`: a
autorização fica no código, perto da rota, e é lida pelo OpenAPI.
