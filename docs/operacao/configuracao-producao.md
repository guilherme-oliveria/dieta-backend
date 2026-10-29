# Configuração de produção (backend)

Guia único de tudo que precisa ser definido para subir a API em produção ou beta: variáveis de ambiente, opções da JVM, banco, rede e passos de segredos. O mobile tem o guia equivalente em `dieta-mobile/docs/operacao/configuracao-producao.md`.

**Regra de ouro:** segredo só em variável de ambiente (arquivo `.env.prod` fora do Git, secrets do GitHub ou do provedor). Nunca em código, imagem ou repositório.

## 1. Como a configuração chega à aplicação

Com o `compose.prod.yaml` (recomendado): você preenche o `.env.prod` (modelo: `.env.prod.example`) e roda

```
docker compose -f compose.prod.yaml --env-file .env.prod up -d --build
```

O compose repassa cada variável ao container `app`. Sem Docker, exporte as mesmas variáveis no ambiente do processo (`systemd`, etc.) e rode `java -jar app.jar` com o perfil `prod`.

Qualquer propriedade `dieta.*` também pode ser trocada por variável de ambiente no formato do Spring: `dieta.auth.login-limite-ip` vira `DIETA_AUTH_LOGIN_LIMITE_IP`. Use isso para os ajustes finos da seção 5.

## 2. Banco de dados (Postgres 18)

| Variável | Obrigatória | Padrão | O que é |
| --- | --- | --- | --- |
| `DB_URL` | sim fora do compose | `jdbc:postgresql://localhost:5432/dietamais` | URL JDBC. No compose é `jdbc:postgresql://postgres:5432/dietamais` (container interno). Para banco externo/gerenciado: `jdbc:postgresql://IP_OU_HOST:5432/NOME?sslmode=require` |
| `DB_USERNAME` | sim | `dietamais` | Usuário da aplicação |
| `DB_PASSWORD` | **sim** | `dietamais` (só dev) | Senha. Em produção use senha forte e única (`openssl rand -base64 24`) |

Pontos de atenção:
- Exige **Postgres 18** (as tabelas usam `uuidv7()`); com versão menor as migrações falham.
- O usuário precisa poder criar objetos no schema (o Liquibase aplica as migrações na subida) e criar triggers/extensões usados pelas migrações.
- No compose o Postgres **não publica porta**: só o `app` e o `backup` o alcançam. Se usar banco externo, libere a porta 5432 apenas para o IP do servidor da API e use TLS (`sslmode=require`).
- O compose do dev (`compose.yaml`) usa a senha `dietamais`; **nunca** reaproveite em produção.
- O pool de conexões (Hikari) usa o padrão do Spring (10). Para mudar: `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE`. Garanta `max_connections` do Postgres acima da soma dos pools de todas as instâncias.

## 3. Variáveis da API

Legenda: **Obrigatória** = a API não sobe ou a função não funciona sem ela.

### Identidade e segurança

| Variável | Obrigatória | Padrão | Como gerar / observação |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | sim | `prod` na imagem | Deve ser `prod` (logs JSON, sem dados de exemplo, sem chaves efêmeras) |
| `JWT_CHAVE_PRIVADA` | **sim** | — | RSA 2048+ em PEM PKCS#8, uma linha com `\n`. `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-privada.pem` e `JWT_CHAVE_PRIVADA="$(awk 'NF{printf "%s\\n",$0}' jwt-privada.pem)"`. Trocar invalida todas as sessões |
| `JWT_CHAVE_PUBLICA` | **sim** | — | `openssl rsa -pubout -in jwt-privada.pem -out jwt-publica.pem` (mesmo formato) |
| `IDEMPOTENCIA_CHAVE` | **sim** | — | 32 bytes em Base64: `openssl rand -base64 32`. Autentica o cache de idempotência e o replay do sync; sem ela escritas idempotentes (e o `POST /sync`) falham. **Diferente** das demais chaves. Trocar invalida os replays guardados |
| `NOTIFICACOES_CHAVE` | **sim** | — | 32 bytes Base64, outra chave. Cifra o conteúdo dos e-mails na fila. Trocar com e-mails pendentes impede o envio deles |
| `ARQUIVOS_SEGREDO` | **sim** (disco local) | — | Segredo que assina os links de PDF/logo/exportação. `openssl rand -base64 32` |
| `HSTS_HABILITADO` | recomendado | `false` | `true` quando o HTTPS é terminado no proxy (o compose já define `true`) |
| `CORS_ORIGENS` | só com site | vazio | Origens HTTPS exatas, separadas por vírgula (ex.: `https://app.exemplo.com.br`). O app mobile não precisa de CORS |
| `WEB_ORIGEM` | só com site | vazio | Origem do site (app web em KMP; cookie de refresh), HTTPS (em desenvolvimento aceita `http://localhost:porta`). Pedido com esta `Origin` recebe o refresh só no cookie |
| `SERVER_FORWARD_HEADERS_STRATEGY` | sim atrás de proxy | `native` no compose | Faz a API enxergar o IP real do cliente (limites por IP). Mantenha `native` com o Caddy/nginx na frente |
| `TAXA_USUARIO_POR_MINUTO` | não | `300` | Limite geral de requisições por usuário por minuto (429 com `Retry-After`) |
| `APP_VERSAO_MINIMA` | não | vazio | Ex.: `1.4.0`. Apps abaixo dessa versão recebem 426 e a tela "atualize o app". Vazio desliga |
| `APP_VERSAO_MINIMA_WEB` | não | vazio | Versão mínima só para `X-Plataforma: WEB` (o site é publicado junto com a API, então normalmente fica vazia) |

### Endereço público e links

| Variável | Obrigatória | Padrão | O que é |
| --- | --- | --- | --- |
| `DOMINIO` | sim (compose) | — | Domínio da API (ex.: `api.dietamais.com.br`), usado pelo Caddy para emitir o certificado e para montar a URL pública. O DNS (registro A) deve apontar para o IP do servidor **antes** da primeira subida |
| `URL_PUBLICA` / `DIETA_PUBLICO_URL_BASE` | **sim** | `http://localhost:8080` | Base HTTPS usada em links enviados por e-mail e em URLs de arquivo (`https://DOMINIO`). O compose já define `DIETA_PUBLICO_URL_BASE` a partir de `DOMINIO` |
| `EMAIL_URL_CONFIRMAR`, `EMAIL_URL_CONVITE`, `EMAIL_URL_REDEFINIR` | não | `dietamais://confirmar-email`, `dietamais://convites`, `dietamais://redefinir-senha` | Links dos e-mails. Com Universal/App Links configurados, use `https://DOMINIO_DOS_CONVITES/...` (veja o guia do mobile, seção de convites) |

### Termos e consentimento (decisão do Guilherme)

- **Termos de uso:** a fonte é `docs/legal/terms-of-use.md`, empacotada na imagem. Enquanto estiver `RASCUNHO`, **ninguém consegue se cadastrar** (`422 TERMOS_INDISPONIVEIS`). Para liberar: revisar o texto, e preencher `status: PUBLICADO`, `aprovadoEm` e `publicadoEm` (UTC ISO-8601, `aprovadoEm` ≤ `publicadoEm`, ambas no passado), commitar e **reconstruir a imagem**. A versão em `versao:` é a que o app envia no aceite.
- `CONSENTIMENTO_VERSAO` e `CONSENTIMENTO_TEXTO` (no compose: `DIETA_CONSENTIMENTO_*`): termo clínico aceito pelo paciente ao entrar por convite (RN-VIN-04). **Sem eles, nenhum paciente aceita convite.** Mudar o texto exige nova versão.

### Admin inicial

| Variável | Padrão | Observação |
| --- | --- | --- |
| `ADMIN_EMAIL` | vazio | E-mail do administrador da plataforma, criado na primeira subida se ainda não existir |
| `ADMIN_SENHA` | vazio | 12+ caracteres. Depois do primeiro login, troque a senha e **remova** a variável do `.env.prod` |

O admin só acessa `/admin/**` (e `/api/v1/eu`, `/auth`, `/legal`).

### E-mail (SMTP)

| Variável | Padrão | Observação |
| --- | --- | --- |
| `EMAIL_ENVIO_HABILITADO` | `false` | `true` para enviar de verdade. **Em produção deve ser `true`**: confirmação de e-mail, convite e recuperação de senha dependem dele |
| `SMTP_HOST` | `localhost` | Servidor SMTP do provedor (Amazon SES, Mailgun, etc.) |
| `SMTP_PORT` | `587` (compose) | 587 com STARTTLS (o compose já liga `starttls` e `auth`) |
| `SMTP_USUARIO` / `SMTP_SENHA` | vazio | Credenciais SMTP (chegam como `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD`) |
| `EMAIL_REMETENTE` | `nao-responda@example.invalid` | Remetente (domínio com SPF/DKIM/DMARC configurados, senão cai no spam) |

A falha do SMTP não derruba a saúde da API: os e-mails ficam na fila e são reenviados com tentativas.

### Pagamento (Asaas)

| Variável | Observação |
| --- | --- |
| `ASAAS_API_KEY` | Chave de API da conta Asaas. **Sem ela, "Assinar" fica indisponível** (`503 PAGAMENTO_INDISPONIVEL`) |
| `ASAAS_WEBHOOK_TOKEN` | Token que o Asaas envia no cabeçalho `asaas-access-token`. Invente um valor forte e cadastre o **mesmo** no painel do Asaas |
| `ASAAS_URL_BASE` | `https://api-sandbox.asaas.com` (teste) ou `https://api.asaas.com` (produção real) |

No Asaas, aponte o webhook para `https://DOMINIO/api/v1/webhooks/pagamento` e ative os eventos `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED`, `PAYMENT_OVERDUE`, `SUBSCRIPTION_DELETED` e `SUBSCRIPTION_INACTIVATED`.

### Push (Firebase Cloud Messaging)

| Variável | Observação |
| --- | --- |
| `PUSH_ENVIO_HABILITADO` | `true` para enviar push |
| `FCM_CREDENCIAIS_JSON` | JSON da **conta de serviço** do projeto Firebase, em uma linha (Console Firebase → Configurações → Contas de serviço → Gerar nova chave). É secreto |

Sem isso a API funciona; só não envia push. O app Android precisa das credenciais do mesmo projeto (veja o guia do mobile).

### Arquivos gerados (PDF, logo, exportação de dados)

| Variável | Padrão | Observação |
| --- | --- | --- |
| `ARQUIVOS_DIR` | `/arquivos` (compose) | Pasta no disco local; o compose monta o volume `arquivos`. Faça backup do volume se usar disco |
| `S3_ENDPOINT` | vazio | **Se preenchido, passa a usar S3/MinIO no lugar do disco.** Ex.: `https://s3.sa-east-1.amazonaws.com` |
| `S3_BUCKET`, `S3_REGIAO` | `dietamais`, `us-east-1` | Bucket e região |
| `S3_CHAVE_ACESSO`, `S3_CHAVE_SECRETA` | vazio | Obrigatórias com `S3_ENDPOINT`. Use um usuário com permissão só nesse bucket |

Com múltiplas instâncias da API, use S3 (disco local não é compartilhado).

## 4. Opções da JVM e do container

A imagem já define:

```
JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
SPRING_PROFILES_ACTIVE=prod
```

- `MaxRAMPercentage=75`: a JVM usa até 75% da memória do container. Defina um limite de memória no container (`mem_limit`/`deploy.resources`); sugestão inicial **1 GB** para o beta, 2 GB com mais carga.
- `ExitOnOutOfMemoryError`: falta de memória derruba o processo e o `restart: unless-stopped` o sobe de novo.
- Para ajustar sem rebuild, sobrescreva `JAVA_TOOL_OPTIONS` no `environment` do serviço `app` (ex.: `-XX:MaxRAMPercentage=60 -Xss512k`). Evite `-Xmx` fixo junto com `MaxRAMPercentage`.
- Fuso: o fuso inicial das contas novas é `dieta.contas.fuso-padrao` (padrão `America/Sao_Paulo`).
- Desligamento ordenado: `server.shutdown=graceful` com 30 s. Dê `stop_grace_period: 40s` ao serviço se fizer deploys frequentes.

## 5. Ajustes finos (opcionais, via `DIETA_...`)

| Propriedade | Padrão | Efeito |
| --- | --- | --- |
| `dieta.auth.login-limite-ip` | 10/min | Tentativas de login por IP |
| `dieta.auth.recuperacao-limite-ip` | 10/min | Recuperação de senha por IP |
| `dieta.convite.limite-ip` / `.janela` / `.validade` | 60 / 1 min / 7 dias | Aceite de convites |
| `dieta.idempotencia.prazo` | 24 h | Quanto tempo uma resposta idempotente fica guardada |
| `dieta.idempotencia.limite-corpo` | 1 MiB | Tamanho máximo do corpo guardado |
| `dieta.assinatura.carencia` | 7 dias | Atraso tolerado antes de bloquear escrita (RN-ASS-04) |
| `dieta.assinatura.aviso-fim-teste` | 3 dias | Aviso antes do fim do teste |
| `dieta.assinatura.tolerancia-webhook` | 3 dias | Espera por webhook perdido antes de marcar atraso |
| `dieta.contas.fuso-padrao` | `America/Sao_Paulo` | Fuso inicial das contas |

## 6. Rede, portas e proxy

- Publicar **apenas** 80 e 443 (Caddy). A API (8080) e o Postgres (5432) ficam em rede interna.
- O Caddy emite o certificado Let's Encrypt sozinho; DNS e portas 80/443 abertas são pré-requisito.
- Saída (egress) que a API usa: SMTP, Asaas (`api.asaas.com`), FCM (`fcm.googleapis.com`) e S3, se usado.
- Health: `GET /actuator/health` (`UP`) e `/actuator/health/readiness`. Só `health` é exposto.
- Swagger/OpenAPI: `SWAGGER_UI_ENABLED=false` em produção (padrão).

## 7. Backup e restauração

| Variável | Padrão | Observação |
| --- | --- | --- |
| `BACKUP_SENHA` | — (**obrigatória**) | Criptografa os dumps (AES-256). **Guarde-a fora do servidor**: sem ela o backup não restaura |
| `DIAS_RETENCAO` | `14` | Dias de dumps mantidos |
| `HORA_BACKUP` | `03:00` | Horário (UTC) do dump diário |

Copie os dumps para fora da VPS (outro provedor/bucket). Teste a restauração periodicamente com `ops/testar-restauracao.sh` (veja `producao.md`).

## 8. Checklist da primeira subida

1. VPS com Docker, DNS do `DOMINIO` apontando para ela, portas 80/443 abertas.
2. Gerar os segredos: par RSA, `IDEMPOTENCIA_CHAVE`, `NOTIFICACOES_CHAVE`, `ARQUIVOS_SEGREDO`, `DB_PASSWORD`, `BACKUP_SENHA`, `ASAAS_WEBHOOK_TOKEN`, `ADMIN_SENHA`. Guardar num cofre de senhas.
3. Preencher `.env.prod`. Obrigatórios: `DOMINIO`, `DB_PASSWORD`, `JWT_*`, `IDEMPOTENCIA_CHAVE`, `NOTIFICACOES_CHAVE`, `ARQUIVOS_SEGREDO`, `BACKUP_SENHA`, `CONSENTIMENTO_*`.
4. **Publicar os termos de uso** (seção 3) e reconstruir.
5. Configurar SMTP e `EMAIL_ENVIO_HABILITADO=true`.
6. Asaas: chave, token e webhook; começar com `ASAAS_URL_BASE` de sandbox.
7. Firebase: `PUSH_ENVIO_HABILITADO=true` e `FCM_CREDENCIAIS_JSON`.
8. `docker compose -f compose.prod.yaml --env-file .env.prod up -d --build` e conferir `https://DOMINIO/actuator/health`.
9. Entrar com o admin, trocar a senha e remover `ADMIN_SENHA` do `.env.prod`.
10. Rodar um backup manual e uma restauração de teste.
11. Gerar os apps apontando para `https://DOMINIO` (guia do mobile).

## 9. O que **não** colocar em produção

`dieta.dev.dados-exemplo`, perfil `dev`, senha `dietamais` do banco, chaves JWT efêmeras, `SWAGGER_UI_ENABLED=true`, `ASAAS_URL_BASE` de sandbox com cobrança real, e qualquer segredo no Git ou em log.
