# Dieta+ — arquitetura do backend

Monólito modular em Java 25 + Spring Boot 4.1, Postgres 18, Liquibase. Um único deploy (Docker) na VPS.
Regras de negócio em `regras-de-negocio.md`; contrato com os clientes em `integracao.md`.

## Módulos

Pacote raiz `com.br.dietamais`. Um pacote por módulo de negócio; cada módulo só expõe o pacote `api` (DTOs e interfaces públicas) para os outros. A fronteira é verificada por teste com **Spring Modulith** (`ApplicationModules.of(DietamaisApplication.class).verify()`); se a versão do Modulith não for compatível com o Boot 4.1, use ArchUnit com a mesma regra.

| Pacote | Responsabilidade | Tabelas principais |
| --- | --- | --- |
| `comum` | Erros (`CodigoErro`, `ExcecaoNegocio`, handler de ProblemDetail), segurança (`UsuarioAtual`, filtro JWT), idempotência, trace id, auditoria (`log_acesso`), entidade base sincronizável | `idempotencia`, `log_acesso` |
| `contas` | Usuários, login, tokens, confirmação de e-mail, senha, dispositivos, preferências | `usuario`, `refresh_token`, `token_uso_unico`, `dispositivo`, `preferencia_notificacao` |
| `assinaturas` | Planos, assinatura, limites, gateway, webhooks | `plano`, `organizacao`, `assinatura`, `evento_pagamento` |
| `pacientes` | Paciente global, vínculo, ficha, convite, consentimento | `paciente`, `vinculo`, `convite`, `consentimento` |
| `alimentos` | Grupos, catálogo base, lista da nutri, ajustes por paciente | `grupo_alimento`, `alimento`, `alimento_nutri`, `ajuste_lista_paciente` |
| `dietas` | Rascunho, publicação, snapshot, orientação geral, dieta em uso | `dieta`, `refeicao`, `item_refeicao`, `dieta_snapshot`, `orientacao` |
| `rotina` | Check-in, água, peso em casa, adesão | `checkin`, `registro_agua`, `registro_peso` |
| `avaliacoes` | Avaliação antropométrica, cálculos (IMC), evolução | `avaliacao` |
| `questionarios` | Modelos, envio, respostas | `modelo_questionario`, `pergunta`, `questionario`, `resposta` |
| `agenda` | Consultas, feed .ics | `consulta`, `calendario_token` |
| `notificacoes` | Outbox de push e e-mail, FCM, SMTP, jobs | `notificacao_pendente` |
| `relatorios` | PDF de dieta e avaliação, exportação LGPD, storage S3 | `arquivo` |
| `sync` | `GET/POST /sync`: junta as mudanças de cada módulo por `seq` e despacha mutações para o módulo dono | (usa as tabelas dos módulos) |
| `admin` | Endpoints do admin da plataforma (planos, assinaturas, CRN) | — |

Dentro de cada módulo:

```
com.br.dietamais.dietas
├── api/          # Controllers REST (DietaController), DTOs (records), eventos públicos (DietaPublicada)
├── aplicacao/    # Casos de uso / services transacionais (PublicarDieta, ServicoDieta)
├── dominio/      # Entidades JPA, enums, regras puras (CalculadoraTotais) — sem Spring quando possível
└── infra/        # Repositories Spring Data, clientes externos, adaptadores do sync
```

Comunicação entre módulos: chamada à interface pública do outro módulo (ex.: `pacientes.api.ConsultaVinculo`) ou **evento de domínio** (`ApplicationEventPublisher` + `@ApplicationModuleListener`), com o registro de publicação de eventos do Modulith em JDBC como outbox. Ex.: `dietas` publica `DietaPublicada` → `notificacoes` grava o push pendente, `agenda` invalida o .ics.

## Isolamento por nutricionista (multi-inquilino)

- Toda tabela clínica tem `nutricionista_id` (não nulo) e `vinculo_id`. O dono do prontuário é a nutri (RN-VIN-01).
- `paciente` é a identidade global; dados de ficha específicos da nutri (objetivo, observações, estatura de referência) ficam na tabela `ficha_paciente` ligada ao vínculo.
- **Nenhum repository tem `findById` exposto para dado clínico.** Use métodos que recebem o dono: `findByIdAndNutricionistaId(id, nid)`. Para o paciente: `findByIdAndPacienteId`.
- `UsuarioAtual` (request-scoped, lido do JWT e validado no banco) é a única fonte de `nutricionistaId`/`pacienteId`. Nunca aceite esses ids do corpo da requisição.
- Não encontrado **ou de outra nutri** → `404` (não revelar existência).
- Teste obrigatório por endpoint: a classe base `TesteIsolamento` cria nutri A e nutri B e confere que B recebe 404 nos recursos de A.
- Postgres Row Level Security pode entrar depois como segunda barreira (BE-31).

## Banco e Liquibase

- Changelog mestre: `src/main/resources/db/changelog/db.changelog-master.yaml`; cada mudança é um arquivo novo `NNN-modulo-descricao.yaml` em `changes/` (próximo: `002-...`). **Nunca edite um changeset que já foi para a main.**
- Tabelas e colunas em `snake_case` português, nome da tabela no singular (`dieta`, `item_refeicao`). PK `id uuid`.
- Novas tabelas usam `defaultValueComputed: uuidv7()` (função nativa do Postgres 18), mas aceitam id vindo do cliente.
- **Colunas padrão de tabela sincronizável**: `versao bigint not null default 0`, `seq bigint not null` (de `seq_sync`), `criado_em`, `atualizado_em timestamptz`, `excluido_em timestamptz null`. Índice em `(nutricionista_id, seq)` e/ou `(paciente_id, seq)`. Na entidade JPA: `@Version` em `versao`, `seq` preenchido por listener (`nextval('seq_sync')`) a cada insert/update.
- Dinheiro `numeric(10,2)`; medidas `numeric(6,2)`; horários `time`; instantes `timestamptz`.
- FKs com nome `fk_<tabela>_<referência>`, únicas `uk_...`, índices `ix_...`.
- Dados de referência (catálogo de alimentos, modelo de anamnese) entram por changeset com `loadData` de CSV em `db/changelog/dados/`.
- Hibernate em `ddl-auto=validate`: se a entidade não bate com o schema, a aplicação não sobe (é o comportamento desejado).

## Segurança

- Spring Security stateless + `oauth2-resource-server` (JWT). Chave de assinatura via variável de ambiente (`JWT_CHAVE_PRIVADA`/`JWT_CHAVE_PUBLICA`); em dev, gerada na subida.
- Senhas com `Argon2PasswordEncoder` (`DelegatingPasswordEncoder` para permitir migrar).
- Autorização por método: `@PreAuthorize("hasRole('NUTRICIONISTA')")` no controller + checagem de dono no service.
- Escrita de nutri passa por `GuardaAssinatura` (RN-ASS-04), aplicado num `HandlerInterceptor` para todo `POST/PUT/PATCH/DELETE` de rotas da nutri e no sync.
- CORS: só a origem do site (`WEB_ORIGEM`). Headers de segurança padrão do Spring + HSTS em produção.
- Segredos só por variável de ambiente (`.env` local, nunca commitado). Ver `.env.example`.
- Logs nunca contêm senha, token, nem dado clínico; use ids.

## Tarefas em segundo plano

- `@Scheduled` + `@SchedulerLock` (ShedLock, tabela `shedlock`) para rodar uma vez mesmo com 2 instâncias.
- Jobs: envio de `notificacao_pendente` (30 s), expiração de teste e carência (diário 03:00), lembrete de consulta (a cada hora), paciente sem check-in há 3 dias (diário 09:00), limpeza de `idempotencia` e tokens vencidos (diário).
- RabbitMQ entra só quando o volume pedir (decisão do Guilherme). Os eventos de domínio já separam os módulos, então a troca é local ao `notificacoes`.

## Integrações externas

| Integração | Interface | Implementação | Dev |
| --- | --- | --- | --- |
| Push | `EnviadorPush` | FCM HTTP v1 (Firebase Admin SDK) | `EnviadorPushLog` (só loga) |
| E-mail | `EnviadorEmail` | SMTP (`spring-boot-starter-mail`) | Mailpit no `compose.yaml` (`http://localhost:8025`) |
| Pagamento | `GatewayPagamento` | Asaas `[SUPOSIÇÃO S3]` | Sandbox do gateway ou `GatewayFalso` |
| Arquivos | `Armazenamento` | S3 compatível (AWS SDK v2) | MinIO no `compose.yaml` |
| PDF | `GeradorPdf` | HTML (Thymeleaf) → PDF (openhtmltopdf) | igual |

Cada interface tem uma implementação falsa para testes, escolhida por `@Profile` ou `@ConditionalOnProperty`.

## API

- Controllers finos: validam entrada (`jakarta.validation`), chamam um caso de uso, devolvem DTO. Nunca devolvem entidade JPA.
- DTOs são `record`s em `api/`, nome `<Coisa>Resposta`, `Criar<Coisa>Requisicao`, `Atualizar<Coisa>Requisicao`.
- `springdoc-openapi` gera `/v3/api-docs`; anote os controllers com `@Tag` e os erros possíveis com `@ApiResponse`.
- Erros: lance `ExcecaoNegocio(CodigoErro.X, "RN-...")`; o `@RestControllerAdvice` converte para ProblemDetail (ver `integracao.md`).

## Testes

| Tipo | Ferramenta | Onde |
| --- | --- | --- |
| Regra pura (cálculo de IMC, totais da dieta, limite do plano) | JUnit 5 + AssertJ | `src/test/.../dominio` |
| API ponta a ponta | `@SpringBootTest` + MockMvc/`RestTestClient` + Testcontainers Postgres 18 | `src/test/.../api` |
| Isolamento entre nutris | `TesteIsolamento` (base) | um por controller |
| Fronteira dos módulos | Spring Modulith `verify()` | `ArquiteturaTest` |
| Contrato | JSON de exemplo em `src/test/resources/contrato/` validado contra o DTO | `ContratoTest` |
| Migrações | a própria subida do contexto com `ddl-auto=validate` | todo teste de integração |

Todo teste roda em `./gradlew test` com Docker ativo; o CI roda `./gradlew build`.

## Ambientes

| Ambiente | Como sobe | Banco |
| --- | --- | --- |
| Local | `./gradlew bootRun` (sobe o `compose.yaml`: Postgres 18, Mailpit, MinIO) | volume Docker |
| Testes | Testcontainers | descartável |
| Produção | Imagem Docker (`bootBuildImage` ou Dockerfile) + `compose.prod.yaml` na VPS com Caddy (HTTPS automático) | Postgres 18 com backup diário criptografado |
