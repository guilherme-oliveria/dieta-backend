# Dieta+ backend — plano de ação

Tarefas em ordem de execução. Cada tarefa = **uma branch + um PR** (`feature/BE-NN-descricao-curta`, saindo de `develop` e voltando para `develop`).
A ordem entre backend, mobile e design (ondas, marcos e releases) está em `plano-de-execucao.md`.
Ao terminar, marque `[x]` neste arquivo **no mesmo PR**.
Dependências em outros repositórios usam os prefixos `MOB-` (dieta-mobile) e `WEB-` (dieta-frontend).
O mobile depende do contrato: publique o endpoint (e o exemplo JSON em `src/test/resources/contrato/`) antes da tarefa MOB correspondente.

Definição de pronto de toda tarefa:
- Changeset Liquibase novo (se mexeu no banco), sem editar os antigos.
- Testes de regra + teste de API + teste de isolamento entre nutris (se o endpoint toca dado clínico).
- Erros com `CodigoErro` e ID da regra; endpoint documentado no OpenAPI.
- `./gradlew build` verde no CI.
- `docs/arquitetura/*.md` atualizados se a tarefa mudou contrato ou regra (e as cópias no mobile/frontend avisadas no PR).

## Fase 1 — Contas e vínculo

- [x] **BE-C1 Contrato da fase 1.** Records de requisição/resposta, controllers anotados no OpenAPI devolvendo `501`, `CodigoErro` novos e um JSON de exemplo por endpoint da fase em `src/test/resources/contrato/`, com teste que valida os exemplos contra os DTOs. Implementação fica nas tarefas da fase. Pode sair antes de BE-01 só com os records e os exemplos; os controllers entram junto com BE-01. *Libera:* o mobile trabalhar com `MockEngine`.
- [x] **BE-01 Base transversal.** Pacotes `comum` e dos módulos vazios; Spring Modulith + `ArquiteturaTest`; `CodigoErro` + `ExcecaoNegocio` + `@RestControllerAdvice` (ProblemDetail com `codigo`, `regra`, `campos`, `traceId`); `springdoc-openapi`; filtro `X-Trace-Id`; suporte a `Idempotency-Key` (tabela `idempotencia`); changeset `002-comum` com `seq_sync`, `idempotencia`, `log_acesso`; classe base `EntidadeSincronizavel` (`versao`, `seq`, `criado_em`, `atualizado_em`, `excluido_em`); Mailpit e MinIO no `compose.yaml`. *Pronto quando:* um endpoint de exemplo lança `ExcecaoNegocio` e o teste confere o JSON do ProblemDetail.
- [x] **BE-02 Cadastro da nutri.** `POST /auth/cadastro-nutri` (RN-CON-01, 02, 04): cria `usuario` (Argon2id), `organizacao` INDIVIDUAL, `nutricionista`, `assinatura` TESTE com `fim_periodo_em = agora + plano.duracao_dias`. Changeset `003-contas`: colunas `email_confirmado_em`, `fuso` em `usuario`; `duracao_dias` em `plano` (TESTE = 30); `crn_verificado` em `nutricionista`. *Depende de:* BE-01.
- [x] **BE-03 Login e tokens.** `/auth/login`, `/auth/refresh` (rotação e detecção de reuso), `/auth/logout`; JWT 15 min com claims `perfil`, `nid`, `pid`; tabela `refresh_token` (hash, dispositivo, família); bloqueio após 5 erros (RN-CON-03); rate limit (Bucket4j ou filtro próprio). Trocar o `httpBasic` do `SecurityConfig` por resource server. *Depende de:* BE-02.
- [ ] **BE-04 E-mail: confirmação e senha.** `token_uso_unico`; `/auth/confirmar-email`, `/auth/esqueci-senha`, `/auth/redefinir-senha` (RN-CON-05, 07); `EnviadorEmail` + templates Thymeleaf; envio via `notificacao_pendente` (cria o módulo `notificacoes` com o job de envio). *Depende de:* BE-03.
- [ ] **BE-05 UsuarioAtual, isolamento e auditoria.** `UsuarioAtual` validado no banco; `TesteIsolamento` base; aspecto/listener que grava `log_acesso` em leitura e escrita de dado clínico (RN-LGPD-01); `GuardaAssinatura` (RN-ASS-04) para escritas da nutri. `GET /eu` com a lista `perfis` (NUTRI e/ou PACIENTE) e `perfil` opcional no login para a mesma conta escolher com qual entrar (tela `EscolherPerfilScreen`). *Depende de:* BE-03.
- [ ] **BE-06 Pacientes.** `GET/POST /pacientes`, `GET/PATCH /pacientes/{id}` com `If-Match`; tabela `ficha_paciente` (changeset `004-pacientes`); `ServicoLimitePlano` (RN-ASS-03, conta CONVIDADO + ATIVO); busca e filtro por status; adesão vazia por enquanto. *Depende de:* BE-05.
- [ ] **BE-07 Convite e aceite.** `convite` (código 8 caracteres sem 0/O/1/I, 7 dias, uso único), `consentimento` (versão do termo, IP); `POST /pacientes/{id}/convite`, `GET /convites/{codigo}`, `POST /convites/{codigo}/aceitar` (cria usuário paciente se preciso, RN-VIN-02 a 04, 07); `POST /pacientes/{id}/encerrar-vinculo` (RN-VIN-05); e-mail do convite. *Depende de:* BE-06. *Libera:* MOB-07, MOB-08.
- [ ] **BE-08 Sync — leitura (esqueleto).** Módulo `sync` com `GET /sync?cursor=`; contrato `FonteSync` que cada módulo implementa (`mudancasDesde(seq, usuario, limite)`); primeiras fontes: `pacientes`/vínculos/nutris; paginação por `temMais`; registros que saíram da visibilidade como `excluido: true`. *Depende de:* BE-06. *Libera:* MOB-09.

## Fase 2 — Alimentos e dieta

- [ ] **BE-C2 Contrato da fase 2.** Records de requisição/resposta, controllers anotados no OpenAPI devolvendo `501`, `CodigoErro` novos e um JSON de exemplo por endpoint da fase em `src/test/resources/contrato/`, com teste que valida os exemplos contra os DTOs. Implementação fica nas tarefas da fase. *Libera:* o mobile trabalhar com `MockEngine`.
- [ ] **BE-09 Catálogo base.** Changeset `005-alimentos` com `grupo_alimento` (nome, kcal, carboidratos, proteínas, gorduras por porção) e `alimento` (grupo, nome, medida caseira, gramas por porção, ativo); CSV de carga inicial em `db/changelog/dados/` extraído do PDF "Lista de substituição" (pasta `doc-dieta` do Guilherme: `C:\projetos-claude\projeto-dieta-mais\doc-dieta`). Se não tiver acesso ao PDF, carregue os 12 grupos com kcal de RN-ALI-02 e deixe os alimentos para um changeset seguinte. `GET /grupos-alimentos`.
- [ ] **BE-10 Lista da nutri e do paciente.** `alimento_nutri` (cópia do catálogo criada no cadastro da nutri, RN-ALI-01) e `ajuste_lista_paciente` (adicionar/remover sobre a lista da nutri); `GET/POST/PATCH /alimentos`, `GET/PUT /pacientes/{id}/lista-substituicao`; desativar em vez de apagar (RN-ALI-03). *Depende de:* BE-09.
- [ ] **BE-33 Perfil da nutri.** `GET/PATCH /eu/nutri` (nome de exibição, CRN, telefone, orientação padrão usada em BE-12) com `If-Match`; logo fica para BE-28. *Depende de:* BE-05. *Libera:* MOB-36.
- [ ] **BE-11 Rascunho de dieta.** `dieta`, `refeicao`, `item_refeicao` (changeset `006-dietas`); `GET/POST /pacientes/{id}/dietas`, `GET/PUT /dietas/{id}` (dieta inteira, com `versao`); `CalculadoraTotais` (RN-DIE-05) com teste usando a dieta de 2509 kcal do PDF como caso de referência. *Depende de:* BE-10.
- [ ] **BE-12 Publicar e duplicar.** `POST /dietas/{id}/publicar` (RN-DIE-02, 03, 06, 08, 09): grava `dieta_snapshot` (JSON imutável com refeições, itens, lista de substituição do paciente e orientação), marca a anterior como SUBSTITUIDA, publica `DietaPublicada`; `POST /dietas/{id}/duplicar` (RN-DIE-04); `orientacao` padrão da nutri. *Depende de:* BE-11.
- [ ] **BE-13 Dieta do paciente.** `GET /eu/dietas` (snapshots publicados de todas as nutris), `PUT /eu/dieta-em-uso` (RN-DIE-07); RN-ASS-05 coberto por teste (nutri com assinatura expirada não afeta a leitura do paciente). *Depende de:* BE-12. *Libera:* MOB-14, MOB-15.
- [ ] **BE-14 Fontes de sync de alimentos e dietas.** `FonteSync` para alimentos da nutri, listas por paciente, dietas (nutri: rascunhos e publicadas; paciente: snapshots). *Depende de:* BE-08, BE-13.
- [ ] **BE-35 Dados de exemplo.** Changeset com `context: dev` (nunca roda em produção): uma nutri, 3 pacientes, a lista de substituição e a dieta de 2509 kcal dos PDFs, uma avaliação. Usado na integração do mobile e nos prints dos PRs. *Depende de:* BE-14.

## Fase 3 — Rotina do paciente

- [ ] **BE-C3 Contrato da fase 3.** Records de requisição/resposta, controllers anotados no OpenAPI devolvendo `501`, `CodigoErro` novos e um JSON de exemplo por endpoint da fase em `src/test/resources/contrato/`, com teste que valida os exemplos contra os DTOs. Implementação fica nas tarefas da fase. *Libera:* o mobile trabalhar com `MockEngine`.
- [ ] **BE-15 Rotina e sync de escrita (inclusão).** `checkin`, `registro_agua`, `registro_peso` (changeset `007-rotina`); `POST /eu/checkins`, `/eu/agua`, `/eu/pesos`; `POST /sync` com operação `CRIAR` para entidades só de inclusão (RN-SYN-03), resultado por item e idempotência por `idMutacao`; `FonteSync` de rotina. *Depende de:* BE-14. *Libera:* MOB-16, MOB-17.
- [ ] **BE-16 Adesão.** `GET /pacientes/{id}/adesao` (% de refeições FEITO/TROCADO nos últimos 7 e 30 dias) e campo `adesao7d` na listagem de pacientes. *Depende de:* BE-15.
- [ ] **BE-17 Feed .ics.** `calendario_token`; `POST /eu/calendario/token`, `GET /cal/{token}.ics` (público, `text/calendar`, VEVENT diário por refeição da dieta em uso, UID estável, título genérico, fuso do usuário). Biblioteca: ical4j ou geração manual com teste de formato. *Depende de:* BE-13. *Libera:* MOB-21.
- [ ] **BE-18 Dispositivos e preferências.** `dispositivo` (token FCM, plataforma), `preferencia_notificacao` (tipos ligados, horário de silêncio); `POST/DELETE /eu/dispositivos`, `GET/PUT /eu/preferencias-notificacao`. *Depende de:* BE-05. *Libera:* MOB-19.

- [ ] **BE-32a Deploy de beta.** A parte mínima de BE-32 num ambiente de beta: imagem Docker, `compose.prod.yaml` (app, Postgres 18, Caddy com HTTPS), backup diário do Postgres, variáveis de ambiente documentadas. Libera o marco M3 (nutri real testando). *Depende de:* BE-16, BE-17, BE-18.

## Fase 4 — Acompanhamento

- [ ] **BE-C4 Contrato da fase 4.** Records de requisição/resposta, controllers anotados no OpenAPI devolvendo `501`, `CodigoErro` novos e um JSON de exemplo por endpoint da fase em `src/test/resources/contrato/`, com teste que valida os exemplos contra os DTOs. Implementação fica nas tarefas da fase. *Libera:* o mobile trabalhar com `MockEngine`.
- [ ] **BE-19 Avaliações.** `avaliacao` (changeset `008-avaliacoes`) com peso, estatura, 9 perímetros e 7 dobras em colunas fixas, composição digitada, parecer, `compartilhada`; IMC e classificação OMS calculados (RN-AVA-02); limites (RN-AVA-03); janela de edição (RN-AVA-04); `GET/POST /pacientes/{id}/avaliacoes`, `PUT /avaliacoes/{id}`, `GET /eu/avaliacoes`; `FonteSync`. *Libera:* MOB-22.
- [ ] **BE-20 Evolução.** `GET /pacientes/{id}/evolucao` e `GET /eu/evolucao`: séries de peso, % gordura, perímetros e peso em casa. *Depende de:* BE-19, BE-15. *Libera:* MOB-23.
- [ ] **BE-21 Modelos de questionário.** `modelo_questionario`, `pergunta` (tipos e condição, RN-ANA-01, 02) e carga do modelo padrão (blocos do doc de arquitetura); `GET/POST/PUT /modelos-questionario`; cópia do padrão no cadastro da nutri. *Libera:* MOB-24.
- [ ] **BE-22 Envio e resposta.** `questionario` (perguntas congeladas, RN-ANA-04), `resposta`; `POST /pacientes/{id}/questionarios`, `GET /eu/questionarios`, `PUT /eu/questionarios/{id}/respostas` (parcial), `POST /eu/questionarios/{id}/enviar` (imutável depois, RN-ANA-03); eventos para push; `FonteSync` e mutação `ATUALIZAR` de respostas do paciente no sync. *Depende de:* BE-21. *Libera:* MOB-25.
- [ ] **BE-23 Consultas.** `consulta` (RN-AGE-01); `GET/POST /consultas`, `PATCH /consultas/{id}`, `GET /eu/consultas`; consultas no feed .ics. *Libera:* MOB-26.
- [ ] **BE-24 Push e jobs de lembrete.** `EnviadorPush` FCM HTTP v1 (credencial por variável de ambiente); listeners que gravam `notificacao_pendente` para os eventos de RN-NOT-01 com payload genérico (RN-NOT-02) respeitando preferências; jobs de consulta amanhã e paciente sem check-in há 3 dias; ShedLock. *Depende de:* BE-18. *Libera:* MOB-27.
- [ ] **BE-34 Central de avisos.** Tabela `aviso` (todo evento de RN-NOT-01 que vira push também fica no histórico do usuário, mesmo com push desligado), `GET /eu/avisos` paginado, `POST /eu/avisos/{id}/lido`; `FonteSync`. *Depende de:* BE-24. *Libera:* MOB-27 (`AvisosScreen`).

## Fase 5 — Offline da nutri

- [ ] **BE-C5 Contrato da fase 5.** Records de requisição/resposta, controllers anotados no OpenAPI devolvendo `501`, `CodigoErro` novos e um JSON de exemplo por endpoint da fase em `src/test/resources/contrato/`, com teste que valida os exemplos contra os DTOs. Implementação fica nas tarefas da fase. *Libera:* o mobile trabalhar com `MockEngine`.
- [ ] **BE-25 Sync de escrita com conflito.** `POST /sync` com `ATUALIZAR`/`EXCLUIR` para entidades da nutri (ficha, alimentos, listas, rascunho de dieta, avaliação, modelos, consultas); conferência de `versaoBase` → `CONFLITO` com `atual`; regras de negócio aplicadas por item (RN-SYN-04); ações que exigem online recusadas no sync. Teste: duas mutações concorrentes na mesma avaliação geram exatamente um conflito. *Libera:* MOB-29, MOB-30.

## Fase 6 — Cobrança, PDF, LGPD e produção

- [ ] **BE-C6 Contrato da fase 6.** Records de requisição/resposta, controllers anotados no OpenAPI devolvendo `501`, `CodigoErro` novos e um JSON de exemplo por endpoint da fase em `src/test/resources/contrato/`, com teste que valida os exemplos contra os DTOs. Implementação fica nas tarefas da fase. *Libera:* o mobile trabalhar com `MockEngine`.
- [ ] **BE-26 Gateway de pagamento.** Interface `GatewayPagamento` + implementação Asaas `[SUPOSIÇÃO S3]` (cliente, assinatura recorrente, checkout hospedado); `POST /assinatura/checkout`, `GET /assinatura` (plano, status, uso x limite); `POST /webhooks/pagamento` validando token e gravando `evento_pagamento` com `evento_id` único (RN-ASS-07). *Libera:* MOB-32.
- [ ] **BE-27 Ciclo de vida da assinatura.** Job diário: TESTE → EXPIRADA, ATIVA → EM_ATRASO → (7 dias) bloqueio; downgrade (RN-ASS-08); push/e-mail de vencimento e recusa. Testes com relógio controlado (`Clock` injetado).
- [ ] **BE-28 PDFs.** `GeradorPdf` (Thymeleaf + openhtmltopdf) para dieta publicada e avaliação, com nome, CRN e logo da nutri; `Armazenamento` S3 (MinIO em dev); `GET /dietas/{id}/pdf`, `GET /avaliacoes/{id}/pdf` devolvendo URL pré-assinada. *Libera:* MOB-33.
- [ ] **BE-29 LGPD.** `POST /eu/exportacao` (JSON + PDFs, gerado em segundo plano, link por e-mail) e `POST /eu/excluir-conta` (anonimização, RN-LGPD-03); soft delete de prontuário pela nutri (RN-LGPD-04). *Libera:* MOB-34.
- [ ] **BE-30 Admin.** Perfil `ADMIN`; endpoints de planos (CRUD), assinaturas (consultar, ajustar com log), marcar `crn_verificado`; sem acesso a dado clínico (RN-LGPD-05), coberto por teste.
- [ ] **BE-31 Endurecimento.** Rate limit geral, CORS por variável, HSTS, `426 VERSAO_APP_OBSOLETA` por `X-App-Versao`, Dependabot, varredura de dependências no CI, RLS do Postgres como segunda barreira (avaliar), revisão OWASP ASVS nível 2.
- [ ] **BE-32 Deploy na VPS.** Dockerfile (ou `bootBuildImage`), `compose.prod.yaml` (app, Postgres 18, Caddy com HTTPS, MinIO ou S3 externo), backup diário criptografado do Postgres com teste de restauração, workflow de deploy no GitHub Actions (tag `v*`), health/readiness, logs em JSON. Domínio a definir. Parte já feita em BE-32a; aqui entra o ambiente de produção, o workflow de deploy e o teste de restauração.

## Pós-MVP (P2, ainda sem tarefas)

Troca de nutri com cópia de histórico (RN-VIN-06), clínicas (organização com várias nutris e secretária), vitrine de nutris e cadastro avulso de paciente, consultoria online paga com split, anúncios, fórmulas da planilha (7 dobras, Pollock e Wilmore) com testes contra a planilha, foto do prato, chat, RabbitMQ quando o volume pedir.
