# Deploy de beta (BE-32a)

Ambiente mínimo para a nutri real testar (marco M3): uma VPS com Docker, domínio apontando para ela e portas 80/443 abertas.

## Subir

1. `cp .env.prod.example .env.prod` e preencha (`DOMINIO`, `DB_PASSWORD`, par RSA do JWT, `IDEMPOTENCIA_CHAVE`, `NOTIFICACOES_CHAVE`; e-mail é opcional no beta). O arquivo fica fora do Git.
2. `docker compose -f compose.prod.yaml --env-file .env.prod up -d --build`
3. Confira: `curl https://$DOMINIO/actuator/health` deve responder `{"status":"UP"}`. O Caddy emite o certificado Let's Encrypt sozinho no primeiro acesso.

O Liquibase aplica as migrações na subida. Para atualizar: `git pull` e repita o passo 2.

## Serviços

| Serviço | O que faz |
| --- | --- |
| `app` | API Spring Boot (perfil `prod`, usuário não-root, readiness em `/actuator/health/readiness`) |
| `postgres` | Postgres 18, volume `pgdata`, sem porta publicada |
| `caddy` | HTTPS e proxy reverso para o `app`; o IP do cliente chega por `X-Forwarded-For` |
| `backup` | `pg_dump` comprimido todo dia às 03:00 UTC em `/backups` (volume `backups`), guarda 14 dias |

## Variáveis (ver `.env.prod.example`)

- `JWT_CHAVE_PRIVADA` / `JWT_CHAVE_PUBLICA`: par RSA 2048+ em PEM; sem elas o app não sobe fora de dev/test.
- `IDEMPOTENCIA_CHAVE`, `NOTIFICACOES_CHAVE`: 32 bytes em Base64 (`openssl rand -base64 32`), cada uma diferente. Sem a primeira, POSTs idempotentes falham.
- `CONSENTIMENTO_VERSAO` / `CONSENTIMENTO_TEXTO`: texto do termo de consentimento clínico (RN-VIN-04). **Decisão do Guilherme**: sem ele, nenhum paciente aceita convite. Em dev há um termo fictício.
- `WEB_ORIGEM`: origem HTTPS do site (só quando o Angular existir).
- `EMAIL_ENVIO_HABILITADO=false` mantém o envio de e-mail desligado (convite e recuperação de senha dependem dele em produção).
- Push (FCM) entra na BE-24; a credencial será outra variável de ambiente.

## Backup e restauração

- Backup manual: `docker compose -f compose.prod.yaml exec backup /bin/sh /ops/backup.sh agora`
- Os dumps saem **criptografados** (`dietamais-*.sql.gz.enc`, AES-256 com `BACKUP_SENHA`). Guarde a senha fora da VPS: sem ela não há restauração. Veja `producao.md` para a cópia externa e o teste de restauração.
- Restaurar (ambiente vazio ou de teste): `openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_SENHA -in dietamais-AAAAMMDDTHHMMSSZ.sql.gz.enc | gunzip | docker compose -f compose.prod.yaml exec -T postgres psql -U dietamais dietamais`

## Limitações conhecidas do beta

Não foi possível rodar `docker compose` no ambiente de desenvolvimento desta entrega (sem Docker): valide o primeiro `up` na VPS e registre ajustes aqui.

## Dados de exemplo (só desenvolvimento)

`./gradlew bootRun --args=--dieta.dev.dados-exemplo=true` cria (uma vez) a nutri `nutri@exemplo.dev`, 3 pacientes (`maria@`, `joao@`, `ana@exemplo.dev`; Maria já vinculada, João com convite pendente) e uma dieta fictícia de ≈2510 kcal publicada para Maria. Senha de todas: `Exemplo de frase para desenvolvimento 2026`. Só roda no perfil `dev`; nunca em produção.
