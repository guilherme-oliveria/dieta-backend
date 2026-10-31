# Dieta+ — arquitetura do backend

## Listas próprias e configuração — BE-10

Migração 012 cria cópias físicas de grupos/alimentos por nutri, marcador de inicialização única, baseline/diferenças por vínculo e histórico de ajustes de macros. Cadastro e backfill copiam a base com valores/proveniência; leitura nunca recopia. A função de inicialização retorna sem alterações quando encontra o marcador, inclusive depois de revisão do catálogo. Macros e alimentos são editados somente com o dono autenticado, guarda, lock e If-Match. O histórico de configuração guarda usuário/data/versões e valores do grupo, sem dados de paciente; auditoria clínica da lista guarda somente identificadores.

alimentos usa a porta pacientes.api.ConsultarFichaParaLista para resolver a ficha própria e seu status, sem acessar infra de outro módulo. Escritas usam o mesmo advisory lock por nutri adotado em pacientes, antes de locks das entidades; GET de lista usa REPEATABLE_READ. Triggers revisam as versões das listas da própria nutri ao mudar alimento/grupo. A sequência seq_versao_lista fornece versões únicas, com saltos permitidos; seq_sync fica separado para BE-14. O diário GET /sync atual não recebeu fontes antecipadas.

Baseline é copiada no vínculo, e diferenças podem incluir alimentos próprios novos somente no paciente escolhido. Inativos não entram na projeção de novas escolhas; não há DELETE por API. Prontuário ENCERRADO mantém leitura e bloqueia alteração de diferenças. BE-12 congelará os dados nos snapshots, sem vínculo dinâmico a alterações futuras de configuração. Ver contrato-fase-2.md/catalogo-base.md para campos opcionais, origem manual e exemplos.

## Diário de leitura incremental (BE-08)

`FonteSync` em `comum.api` separa seleção de referências e carga dos DTOs autorizados. Fontes dos módulos pacientes/contas usam o diário comum sem dependência circular entre módulos. O serviço sync escolhe a página global antes de carregar dados e audita acessos clínicos autorizados, inclusive tombstones, sem conteúdo clínico no diário ou na auditoria.

A migração 009 acrescenta `diario_sync` por destinatário/perfil e `estado_sync` com marco confirmado. Triggers publicam mudanças de ficha, vínculo e identidade pública da nutri, incluindo reatribuição de paciente provisório. A transação escritora bloqueia a linha única do estado antes de alocar revisões do diário e mantém o bloqueio até commit. Assim, outra escrita não publica revisão posterior antes de uma anterior ainda não confirmada. Lacunas na sequência são permitidas.

A leitura usa `REPEATABLE_READ`: marco, referências e projeções pertencem ao mesmo snapshot, sem esperar transações ainda não confirmadas. Projeções retornam somente o estado atualmente autorizado; perda de visibilidade gera tombstone e ENCERRADO mantém histórico. Revisão do envelope e versão da entidade são distintas, conforme `integracao.md`.

O bloqueio global serializa a publicação do diário, escolha deliberada para correção no MVP. Novas escritas devem manter os bloqueios de negócio antes das alterações que disparam triggers e uma ordem consistente; não adquirir bloqueios de negócio de outra operação após publicar no diário. Fontes futuras precisam registrar todas as alterações que afetam sua projeção na mesma transação. Não há mensageria nem retenção/expurgo do diário nesta entrega.

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

BE-06 usa `ficha_paciente` (migração 007) por vínculo, com `id = vinculo_id`, dono, paciente, dados particulares e metadados de sync. Consultas exigem simultaneamente o dono da ficha e do vínculo, com o mesmo paciente. Alterar a ficha nunca altera a identidade global. Cadastros bloqueiam a organização no `ServicoLimitePlano` (porta `ReservaVagaPaciente` em `comum.api`, sem ciclo entre módulos); escritas da mesma nutri usam bloqueio consultivo transacional para serializar duplicidade de e-mail. PATCH bloqueia ficha/vínculo antes de comparar a versão e atualiza versão/seq/Clock em uma transação. O limite de plano conta vínculos pendentes e ativos por organização; a edição não consome vaga.
- Teste obrigatório por endpoint: a classe base `TesteIsolamento` cria nutri A e nutri B e confere que B recebe 404 nos recursos de A.
- Postgres Row Level Security pode entrar depois como segunda barreira (BE-31).

## Banco e Liquibase

- Changelog mestre: `src/main/resources/db/changelog/db.changelog-master.yaml`; cada mudança é um arquivo novo `NNN-modulo-descricao.yaml` em `changes/` (próximo: `006-...`). **Nunca edite um changeset que já foi para a main.**
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

### E-mail e recuperação entregues em BE-04

`005-email-tokens.yaml` cria `token_uso_unico` e `notificacao_pendente`. `EmitirTokenConta` cria 32 bytes aleatórios, guardados somente como SHA-256; prazos de 24 h (RN-CON-05) e 1 h (RN-CON-07) usam Clock. O cadastro enfileira a confirmação na mesma transação da conta e da idempotência. `GerenciarEmailSenha` bloqueia o usuário e relê o token para conferir finalidade/expiração/uso/anulação antes de confirmar ou redefinir; senha fraca não consome o token. Redefinição revoga todos os refresh do usuário, sem login automático. A limpeza de sessões também apaga tokens expirados.

Recuperação retorna sempre 202 para e-mail válido. O TransactionTemplate desfaz falha de emissão/fila antes da resposta igual; o log só contém traceId. Nova solicitação anula os tokens anteriores da mesma finalidade. Quota IP separada da de login: 10/min, `dieta.auth.recuperacao-limite-ip`.

`contas` usa só `notificacoes.api.EnfileirarEmail`; propagação MANDATORY garante atomicidade. A outbox cifra destinatário/token/prazo com AES-256-GCM, nonce aleatório e autenticação pelo ID/tipo. `NOTIFICACOES_CHAVE`: 32 bytes aleatórios em Base64, estáveis e independentes da chave de idempotência. Sem chave o cadastro falha com rollback, sem texto puro. Rotação exige drenar/invalidar pendências em manutenção; não há leitura por várias chaves nesta etapa.

`ProcessarEmails` usa FOR UPDATE SKIP LOCKED, templates Thymeleaf e SMTP por EnviadorEmail/JavaMailSender. Job de 30 s (`dieta.notificacoes.intervalo-envio`), até 10 mensagens/lote e 5 tentativas com espera de 30/60/120/240 s. Remove payload após envio/expiração/falha definitiva; expiração roda mesmo com envio desabilitado. SMTP + marcação ficam na transação, mas crash após SMTP/antes do commit pode duplicar entrega. O link permanece de uso único.

Em dev: configurar NOTIFICACOES_CHAVE, subir Compose extras e habilitar EMAIL_ENVIO_HABILITADO=true. O opt-in é falso por padrão. SMTP padrão localhost:1025; host/porta/remetente/links em `.env.example`, autenticação/TLS por propriedades Spring Mail no ambiente. Timeouts: conexão/escrita 5 s, leitura 3 s. Links vêm somente de configuração (app ou HTTPS), nunca Host do cliente. Não há dados clínicos nos e-mails.

Testcontainers usa cifra real com chave aleatória em memória, agendamento de envio desabilitado e EnviadorEmail falso. EmailSenhaApiTest usa HTTP/Postgres reais para efeitos, rollback e concorrência; teste MIME injeta apenas transporte externo mockado. O teste de template usa o mesmo SpringTemplateEngine/Spring EL do starter Boot, sem acrescentar OGNL.

### Login e tokens entregues em BE-03

`AutenticarSessao` usa `TransactionTemplate`: tentativas bloqueadas e revogações por reuso são confirmadas antes de lançar a exceção que vira HTTP 401/429. Erro de gravação ou de assinatura ainda desfaz toda a emissão. `SessaoStore` mantém a projeção de autenticação via JDBC; não expõe entidades nem substitui o `UsuarioAtual` de BE-05. Bloqueio de uma linha por identificador normalizado serializa a janela móvel RN-CON-03 mesmo para e-mail desconhecido. O banco guarda hash do identificador e as últimas falhas; a comparação de senha inexistente usa um hash Argon2id fictício criado só em memória, com o mesmo custo do encoder real.

`004-sessoes.yaml` cria `refresh_token` (hash, usuário, perfil, instalação, plataforma, nome do aparelho, família, expiração, consumo e revogação) e `bloqueio_login`. A transação bloqueia o usuário antes de reler o refresh: só uma requisição concorrente consome o token; a outra detecta reuso e revoga a família, inclusive o sucessor já emitido. Novo login na mesma instalação revoga suas famílias anteriores; dispositivos diferentes permanecem independentes. O prazo de 30 dias é absoluto por família. O job `LimpezaSessoes` remove famílias vencidas e tentativas inativas há um dia, com intervalo configurável em `dieta.auth.intervalo-limpeza` (PT1H). Exclusões são idempotentes entre instâncias.

O resource server valida assinatura RS256, emissor/audiência, identidade/perfil e expiração usando `Clock`, sem tolerância após o prazo. Só o perfil ativo vira `ROLE_*`; claims da outra identidade não conferem permissão. `TokensJwt` emite 15 minutos. O par RSA vem das variáveis descritas em `.env.example`; ausência de chaves só permite gerar um par efêmero em `dev`/`test`. O perfil padrão é `dev`; produção deve definir `SPRING_PROFILES_ACTIVE=prod` e fornecer ambas. Não há chave persistida ou registrada em log. Recursos protegidos rejeitam Basic; as rotas de sessão autenticarão por senha/refresh e devem ser chamadas sem Bearer expirado.

`FiltroTaxaLogin` roda após rastreio, antes da autenticação: padrão 10/min por IP real da conexão, configurável por `dieta.auth.login-limite-ip`, memória limitada a 10 mil IPs e recusa segura ao atingir a capacidade. Não confia em headers de proxy. É proteção local por processo; o bloqueio RN-CON-03 é persistente entre instâncias. A política de proxy confiável/limite geral continua no endurecimento BE-31. O cookie de refresh exige Origin HTTPS configurada e exata para ser lido, além de `HttpOnly; Secure; SameSite=Strict`; o app continua usando JSON.

A observação de concorrência do QA de BE-02 foi incorporada: o insert de usuário agora usa `ON CONFLICT DO NOTHING` sem alvo, arbitrando tanto a constraint original de e-mail quanto o índice normalizado, sem editar migrations já aprovadas. O teste concorrente continua exigindo exatamente 201/409. Nenhum JSON ou nome de campo foi alterado, e login não exige e-mail confirmado (essa restrição é do primeiro convite, RN-CON-05). Login/refresh não recebem cache idempotente.

### Cadastro de nutri entregue em BE-02

`CadastrarNutri` aplica RN-CON-01/02/04 numa transação: valida senha/fuso, gera UUIDv7 pelo Postgres 18, grava usuário, chama a interface pública `CriarOrganizacaoIndividual` de assinaturas e cria a identidade da nutri. O módulo de assinaturas lê o plano `TESTE` ativo, cria a organização `INDIVIDUAL` e calcula `fimPeriodoEm` a partir do instante fornecido pelo `Clock` e de `plano.duracao_dias`. Limite e duração são dados do plano. IDs entre módulos são referências UUID, sem expor entidades JPA. Flushes preservam a ordem das FKs sem confirmar a transação antes da resposta e do cache idempotente.

`003-contas.yaml` acrescenta as colunas de confirmação/fuso do usuário, a duração do plano (carga inicial TESTE = 30) e `crn_verificado`. Também guarda a versão declarada/instante do aceite dos termos que já fazem parte da requisição; esses dados ficam nulos em usuários antigos, sem fabricar consentimento. O índice único `lower(btrim(email))` arbitra a unicidade global. A inserção usa `ON CONFLICT DO NOTHING`: concorrência vira `EMAIL_JA_CADASTRADO`, sem deixar órfãos nem provocar exceção SQL que registre o e-mail. `Usuario` tem mapeamento JPA para validação de schema e leitura; o cadastro usa esse insert específico para arbitrar o conflito.

O `DelegatingPasswordEncoder` usa Argon2id (salt aleatório, 19 MiB, duas iterações, paralelismo 1) e Bouncy Castle. A lista local de hashes de senhas comuns vem de uma revisão fixada de SecLists/NCSC; origem, transformação e licença estão em `src/main/resources/seguranca/README.md`. A propriedade `dieta.contas.senhas-bloqueadas` permite substituir o recurso; senha alguma é enviada a serviços externos. O SHA-256 serve somente à consulta da lista pública, nunca à persistência da credencial. Fuso padrão configurável: `dieta.contas.fuso-padrao` (America/Sao_Paulo).

Somente POST `/api/v1/auth/cadastro-nutri` é liberado ao público nesta tarefa. O controller usa `@Valid`, o erro segue ProblemDetail e `@Idempotente` reaproveita o cadastro sem duplicá-lo. Sem a chave de cifra configurada, uma requisição idempotente que falhe ao gravar a resposta desfaz também usuário/organização/assinatura/nutri. Não cria sessão automaticamente; BE-03 implementa login/JWT e BE-04 enfileira a confirmação. O caso de uso não aceita usuário, organização, plano ou perfil escolhidos pelo corpo; o novo perfil é sempre NUTRICIONISTA.

Como o cadastro recebe credenciais, o fingerprint persistido pela idempotência é autenticado com HMAC-SHA-256, usando uma chave derivada por finalidade da mesma `IDEMPOTENCIA_CHAVE`. Inclui escopo e chave da operação, além dos metadados/corpo, evitando que o banco contenha um verificador rápido de senha baseado só em SHA-256. Sem a chave configurada, o fingerprint temporário dos stubs/erros não é persistido, pois qualquer resposta 2xx falha na cifra e desfaz a transação. Não há nova variável ou campo no contrato.

### Base transversal entregue em BE-01

- Spring Modulith 2.1.1 verifica os módulos e suas interfaces nomeadas `api` em `ArquiteturaTest`, incluindo ausência de ciclos. Os tipos compartilhados `Perfil`, `Plataforma`, `UsuarioResposta`, `SessaoResposta` e `DispositivoRequisicao` ficam em `comum.api`: evita o ciclo contas → pacientes → contas, mantendo JSON e nomes de schema OpenAPI da BE-C1. O manifesto muda só o nome Java desses tipos.
- `ExcecaoNegocio` e `TratadorErros` produzem Spring `ProblemDetail` com `codigo`, `regra`, `campos`, `traceId` e `atual`. O campo RFC padrão `instance` pode ser acrescentado pelo Spring. Bean Validation só informa campo/código/mensagem segura; nunca retorna o valor rejeitado. Falhas inesperadas usam mensagem genérica e logam somente o rastreio. O endpoint de exemplo que lança exceção existe apenas no teste, sem rota de diagnóstico publicada.
- `FiltroRastreio` roda antes da segurança e restaura o MDC ao encerrar. Erros 401/403 também usam o contrato. Os stubs continuam 501 e o Basic será substituído em BE-03. Endpoints reais devem usar `@Valid @RequestBody`; o teste da base já verifica esse caminho.
- `@Idempotente` habilita as operações de criação; o filtro roda depois da segurança. `ServicoIdempotencia` mantém reserva, operação e resposta 2xx numa única transação. A restrição única `(escopo, chave)` serializa concorrentes; erros desfazem a transação. Escopo deriva do usuário/perfis autenticados, nunca de um header de dono. A requisição é limitada a 1 MiB por padrão, mantida em memória para desserialização e persistida apenas como hash SHA-256. O replay inclui corpo, status, Content-Type, Location, ETag e Cache-Control, com o rastreio da nova requisição. Não aplica a login/refresh nem armazena erros/stubs.
- `dieta.idempotencia.prazo` (padrão `PT24H`), `dieta.idempotencia.limite-corpo` (1048576 bytes) e `dieta.idempotencia.intervalo-limpeza` (padrão `PT1H`) são configuráveis. Um job remove respostas expiradas; a remoção é idempotente e segura em múltiplas instâncias. Não há lock distribuído para esse delete. Os jobs futuros com efeito externo continuam previstos com ShedLock.
- O corpo da resposta idempotente é cifrado com AES-256-GCM do JDK, nonce aleatório e autenticação vinculada ao escopo/chave/hash. Assim o futuro aceite não cria uma cópia de refresh token em texto puro na tabela de cache. `IDEMPOTENCIA_CHAVE` deve conter 32 bytes aleatórios codificados em Base64, estáveis entre reinícios e instâncias, guardados fora do banco/Git/log. A aplicação pode iniciar sem a chave, mas uma gravação idempotente 2xx falha com rollback e erro seguro se ela não estiver configurada; stubs 501 não a exigem. Troca da chave exige aguardar o prazo do cache ou invalidá-lo em manutenção, pois não há rotação com múltiplas chaves nesta primeira versão. Testes criam suas chaves aleatórias apenas em memória.
- `002-comum.yaml` acrescenta `seq_sync`, `idempotencia` e `log_acesso`, com PK UUIDv7 para tabelas novas. O log guarda metadados de acesso; seu preenchimento é BE-05. Não tem FK para usuários/prontuários, preservando a trilha após anonimização. Não guarda corpo clínico nem tokens. A tabela de idempotência contém respostas restritas ao escopo e expiradas após o prazo; deve ter a mesma proteção de backup/acesso do banco.
- `EntidadeSincronizavel` fornece versão JPA, sequência e timestamps. O listener usa `Clock` e `JdbcTemplate` na conexão transacional para obter `nextval('seq_sync')`, evitando consultas JPA recursivas durante flush. A subclasse define o próprio ID e exclusão lógica recebe instante do relógio do caso de uso. O teste JPA usa entidade/tabela apenas em `src/test`, fora do pacote varrido em produção.
- Compose acrescenta Mailpit em `127.0.0.1:1025/8025` e MinIO em `127.0.0.1:9000/9001`, ambos no perfil opcional `extras`. `./gradlew bootRun` sobe somente o Postgres por padrão; para os serviços adicionais, execute `docker compose --profile extras up -d`. `MINIO_ROOT_USER` e `MINIO_ROOT_PASSWORD` aceitam valores do ambiente e usam defaults exclusivos de desenvolvimento quando ausentes ou vazios; `.env.example` pode ser copiado sem preencher esses campos. Esses defaults não devem ser usados em produção. `MAILPIT_IMAGE` e `MINIO_IMAGE` permitem configurar as imagens. O MinIO usa uma release fixada do registro oficial Quay para desenvolvimento; disponibilidade do pull e execução precisam ser verificadas num ambiente com Docker. Não é recomendação de imagem para produção. Boot ignora ambos ao inferir conexões; o adaptador SMTP entra em BE-04 e S3 em sua tarefa.

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

## Módulo `referencias` (BE-39)

Tabelas de referência clínica como **dado**: `referencia_faixa` (changeset `030-referencias`, carga do CSV `db/changelog/dados/referencias-adulto.csv`) com IMC adulto e idoso, risco de comorbidades, cintura, compleição e Pollock e Wilmore (% de gordura). Cada linha tem `fonte` e `versao_referencia` (hoje `adulto-1.0.0`). Faixa de idade é [min, max); faixa de valor tem marcadores de limite inclusivo (ex.: IMC de idoso normal 23 a 28 fechado). Outros módulos só usam `referencias.api.ConsultaReferencias`; os testes de cálculo usam `TabelaFaixas.doCsv` sobre o mesmo CSV. Corrigir uma faixa = novo changeset (nunca editar o 030) com nova `versao_referencia`. Pressão arterial entra no BE-57.
