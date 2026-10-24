# Dieta+ — backend

API do Dieta+ em Java 25 e Spring Boot 4.1, com Postgres 18 e schema versionado pelo Liquibase.

## Pré-requisitos

- JDK 25
- Docker (Docker Desktop no Windows)

## Rodando localmente

```bash
./gradlew bootRun
```

O Spring Boot lê o `compose.yaml` e sobe o Postgres 18 sozinho antes de iniciar a aplicação. Se quiser subir o banco à mão:

```bash
docker compose up -d
```

Para mudar usuário, senha ou porta do banco, copie `.env.example` para `.env` e ajuste.

Outra opção, sem `compose.yaml`: rode a classe `TestDietamaisApplication` (em `src/test`), que sobe um Postgres 18 temporário via Testcontainers.

A API fica em `http://localhost:8080`. Health check: `GET /actuator/health`.

## Testes

```bash
./gradlew test
```

Os testes usam Testcontainers e sobem um Postgres 18 próprio; o Docker precisa estar rodando.

## Banco de dados e Liquibase

- Changelog principal: `src/main/resources/db/changelog/db.changelog-master.yaml`.
- Cada mudança é um arquivo novo em `db/changelog/changes/`, com prefixo numérico (`002-...yaml`, `003-...yaml`). Eles rodam em ordem alfabética.
- Nunca edite um changeset que já rodou; crie um novo.
- O Hibernate só valida o schema (`ddl-auto=validate`); quem cria e altera tabelas é o Liquibase.

Schema inicial (`001-contas-e-planos.yaml`): `plano` (com os planos Teste, 5 pacientes, e Básico, R$ 29,90 e 20 pacientes), `organizacao`, `usuario`, `nutricionista`, `paciente`, `vinculo` e `assinatura`.

## Configuração por ambiente

| Variável | Padrão local |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/dietamais` |
| `DB_USERNAME` | `dietamais` |
| `DB_PASSWORD` | `dietamais` |
