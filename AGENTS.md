# AGENTS.md — dieta-backend

Instruções para agentes de código (Codex, Claude Code e outros). O `CLAUDE.md` importa este arquivo; edite só aqui.

## O projeto

Dieta+: app para nutricionistas e pacientes (multi-nutri, com assinatura). Este repositório é a **API**: única fonte de verdade e de regras de negócio.
Repositórios irmãos: `guilherme-oliveria/dieta-mobile` (KMP: Android, iOS, Desktop) e `guilherme-oliveria/dieta-frontend` (Angular, pós-MVP).
Idioma: **português** em docs, commits, PRs, mensagens de erro e nomes de domínio. Termos técnicos (controller, repository) ficam em inglês.

## Leia antes de começar

1. `docs/arquitetura/visao-geral.md`: produto, roadmap, decisões e suposições.
2. `docs/arquitetura/plano-de-acao.md`: **a lista de tarefas**. Pegue a primeira `[ ]` cujas dependências estão `[x]`, a menos que o pedido diga outra.
3. `docs/arquitetura/regras-de-negocio.md`: regras `RN-xxx` que você precisa aplicar.
4. `docs/arquitetura/integracao.md`: contrato com os clientes (auth, erros, sync, push, .ics).
5. `docs/arquitetura/backend.md`: módulos, Liquibase, segurança, testes.
6. `docs/arquitetura/funcionalidades-mvp.md`: critérios de aceite (`F-xx`).

Os arquivos `visao-geral`, `regras-de-negocio`, `integracao` e `funcionalidades-mvp` são **comuns aos 3 repositórios**; a cópia oficial é esta. Se mudar um deles, diga no PR para atualizar as cópias no mobile e no frontend.

## Stack

Java 25 · Spring Boot 4.1.2 (webmvc, data-jpa, security, validation, actuator) · Gradle Kotlin DSL · Postgres 18 · Liquibase (YAML) · Testcontainers · Docker Compose. Pacote raiz `com.br.dietamais`.

## Comandos

| Ação | Comando |
| --- | --- |
| Rodar (sobe Postgres pelo `compose.yaml`) | `./gradlew bootRun` |
| Testes (precisa de Docker) | `./gradlew test` |
| Build completo (o que o CI roda) | `./gradlew build` |
| Só o banco | `docker compose up -d` |
| Rodar com Postgres de Testcontainers | classe `TestDietamaisApplication` em `src/test` |

Health: `GET http://localhost:8080/actuator/health`. OpenAPI (a partir de BE-01): `/v3/api-docs`.

## Regras inegociáveis

- **O backend decide.** Toda permissão, limite de plano e regra é validada aqui, em toda requisição e também no `/sync`.
- **Isolamento:** dado clínico sempre filtrado pelo dono (`nutricionista_id` ou `paciente_id` vindo de `UsuarioAtual`). Nunca use id do corpo da requisição para isso. De outra nutri = `404`. Todo endpoint novo de dado clínico tem teste de isolamento (`TesteIsolamento`).
- **Liquibase:** mudança de banco = arquivo novo em `src/main/resources/db/changelog/changes/` com o próximo número. Nunca edite changeset que já está na `develop` ou na `main`. Hibernate fica em `ddl-auto=validate`.
- **Erros:** `ExcecaoNegocio(CodigoErro.X, "RN-...")` → ProblemDetail. Código novo também vai para `docs/arquitetura/integracao.md`.
- **Contrato:** mudança incompatível de API só em nova versão (`/api/v2`). Campos novos são opcionais.
- **Segredos** só em variáveis de ambiente. Nada de chave, senha ou token no Git. Não logue senha, token nem dado de saúde.
- Não adicione mensageria (RabbitMQ/Kafka), microsserviço ou Keycloak sem pedido explícito: está decidido que não entram no MVP.

## Convenções

- Um pacote por módulo (`contas`, `pacientes`, `dietas`...), cada um com `api/`, `aplicacao/`, `dominio/`, `infra/`. Outros módulos só usam o `api/` (verificado por `ArquiteturaTest`).
- DTOs são `record`s (`CriarPacienteRequisicao`, `PacienteResposta`). Controller nunca devolve entidade.
- Tabelas `snake_case` no singular em português; PK `uuid` com `uuidv7()`; tabelas sincronizáveis com `versao`, `seq`, `criado_em`, `atualizado_em`, `excluido_em`.
- Cite a regra no código só quando ajuda: `// RN-ASS-03: convites pendentes também ocupam vaga`.
- Use `Clock` injetado para tudo que depende de data (testável).
- Siga o estilo do código ao redor (tabs, como o código gerado pelo Initializr).

## Fluxo de trabalho

1. Branch a partir da `develop` (Gitflow; ver `docs/arquitetura/plano-de-execucao.md`), com PR de volta para `develop`: `feature/BE-NN-descricao-curta` (ex.: `feature/BE-02-cadastro-nutri`). Se o ambiente já definiu uma branch para você, use a dele.
2. Commits pequenos, mensagem em português no imperativo (`Adiciona cadastro da nutri`).
3. Rode `./gradlew build` antes do PR. Se não houver Docker no ambiente, diga isso no PR e confie no CI.
4. PR com o template `.github/pull_request_template.md`: tarefa (`BE-NN`), regras aplicadas, mudanças de contrato, como testar.
5. No mesmo PR, marque a tarefa como `[x]` em `docs/arquitetura/plano-de-acao.md`.
6. Uma tarefa por PR. Se a tarefa for grande demais, divida em `BE-NNa`, `BE-NNb` no plano.

## Definição de pronto

Changeset novo (se mexeu no banco) · testes de regra, de API e de isolamento · OpenAPI anotado · `CodigoErro` com ID da regra · CI verde · docs atualizados se mudou contrato/regra.

## Suposições

Pontos ainda não confirmados com o Guilherme estão marcados `[SUPOSIÇÃO Sx]` em `visao-geral.md`. Implemente como está escrito, deixando o valor configurável (tabela ou propriedade), e não bloqueie a tarefa por causa deles.
