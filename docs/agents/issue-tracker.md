# Issue tracker: GitHub

As issues da Fase 4, inclusive as do Billing, vivem no repositório **`15SOAT-JVGG/oficina-app`**
(a spec é a [#106](https://github.com/15SOAT-JVGG/oficina-app/issues/106); o catálogo é a
[#107](https://github.com/15SOAT-JVGG/oficina-app/issues/107)). Como este repositório é outro, o
`gh` **não infere o repo pelo remote**: passe sempre `--repo 15SOAT-JVGG/oficina-app` nas operações
de issue até que a organização decida mover as issues do Billing para cá.

## Operações

- **Ler uma issue:** `gh issue view <n> --repo 15SOAT-JVGG/oficina-app --comments`
- **Listar as do Billing:** `gh issue list --repo 15SOAT-JVGG/oficina-app --state open --search "Billing" --json number,title,labels`
- **Comentar (plano, pendência, evidência):** `gh issue comment <n> --repo 15SOAT-JVGG/oficina-app --body-file <arquivo.md>`
  — use arquivo para corpos longos; nunca interpole conteúdo não confiável na linha de comando.
- **Rótulos:** `gh issue edit <n> --repo 15SOAT-JVGG/oficina-app --add-label "..."` / `--remove-label "..."`.
  O rótulo `ready-for-agent` marca uma issue autocontida; retire-o ao assumir e devolva-o se
  precisar devolver a issue.
- **Fechar:** só por PR mergeado com `Closes 15SOAT-JVGG/oficina-app#<n>` na descrição, ou
  `gh issue close <n> --repo 15SOAT-JVGG/oficina-app --comment "..."` quando a issue for
  explicitamente descartada.

## Pull requests

PRs são abertos **neste** repositório (`oficina-billing`), referenciando a issue do `oficina-app`
pelo nome completo (`15SOAT-JVGG/oficina-app#107`). Título em Conventional Commits; descrição no
formato de evidências de [workflow.md](workflow.md).

- **Abrir:** `gh pr create --title "feat: ..." --body-file <descricao.md>`
- **Ler para revisar:** `gh pr view <n> --comments` e `gh pr diff <n>`
- **Checks:** `gh pr checks <n>` — nenhum merge com check vermelho.

**PRs como superfície de triagem: não.** PR externo não é pedido de funcionalidade.

## Quando um documento diz "registre na issue"

Comente na issue do `oficina-app` com `gh issue comment`. Pendências de
[`docs/architecture/domain-decisions.md`](../architecture/domain-decisions.md) são registradas
assim e, quando respondidas, a resposta volta para o documento no mesmo PR.
