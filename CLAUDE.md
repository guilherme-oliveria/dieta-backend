# CLAUDE.md — dieta-backend

As instruções do projeto ficam no AGENTS.md (compartilhado com o Codex). Edite lá, não aqui.

@AGENTS.md

## Só para o Claude Code

- Comece lendo `docs/arquitetura/plano-de-acao.md` e diga qual tarefa (`BE-NN`) vai fazer.
- Em ambiente na nuvem sem Docker, os testes com Testcontainers não sobem: escreva os testes mesmo assim e confie no CI (`./gradlew build`).
- Se o Maven Central responder 429, repita com `./gradlew build --max-workers=1`.
