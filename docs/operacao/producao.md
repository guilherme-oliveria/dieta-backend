# Produção (BE-32)

Complementa o [deploy de beta](deploy-beta.md). Tudo aqui está no repositório; o que depende de conta, domínio ou segredo do Guilherme está marcado **(Guilherme)**.

## O que é preciso ter

1. **(Guilherme)** Uma VPS Linux com Docker e Compose (2 vCPU e 4 GB bastam para começar), portas 80 e 443 abertas e só SSH por chave.
2. **(Guilherme)** Um domínio com registro `A` apontando para a VPS (`DOMINIO`). O Caddy emite o certificado sozinho.
3. **(Guilherme)** Conta no Asaas (chave e token do webhook), projeto Firebase (push) e SMTP, quando for ligar cada um. Nenhum é obrigatório para subir; sem eles o recurso correspondente fica indisponível (checkout, push, e-mail).
4. `cp .env.prod.example .env.prod` e preencha. Gere `ARQUIVOS_SEGREDO`, `BACKUP_SENHA`, `IDEMPOTENCIA_CHAVE` e `NOTIFICACOES_CHAVE` com `openssl rand -base64 32`. O arquivo fica fora do Git e com permissão `600`.

## Primeira subida

```
docker compose -f compose.prod.yaml --env-file .env.prod up -d --build
curl https://$DOMINIO/actuator/health/readiness     # {"status":"UP"}
```

- O Liquibase cria o schema. Com `ADMIN_EMAIL` e `ADMIN_SENHA` a conta de administração é criada na primeira subida; depois **remova `ADMIN_SENHA` do `.env.prod`** e troque a senha pelo fluxo de redefinição.
- No Asaas, aponte o webhook para `https://$DOMINIO/api/v1/webhooks/pagamento` com o cabeçalho `asaas-access-token` igual a `ASAAS_WEBHOOK_TOKEN`.
- Logs saem em JSON (formato ECS, uma linha por evento, com `traceId`): `docker compose -f compose.prod.yaml logs -f app`.

## Deploy por tag

O workflow `ops/github/workflows/deploy.yml` (copie para `.github/workflows/`) roda o build completo, publica a imagem no GHCR e atualiza a VPS por SSH ao criar uma tag `vX.Y.Z` na `main`. Segredos do repositório: `VPS_HOST`, `VPS_USUARIO`, `VPS_CHAVE_SSH`, `VPS_DIRETORIO`. Antes de subir a nova versão ele faz um backup e, depois, confere o readiness. Rollback: `git checkout vANTERIOR` na VPS e repetir o `up -d --build` (as migrações são aditivas; nunca se edita changeset publicado).

## Backup e restauração

- Todo dia às 03:00 UTC o serviço `backup` grava `dietamais-*.sql.gz.enc` (dump comprimido e criptografado com `BACKUP_SENHA`) em `/backups` e guarda `DIAS_RETENCAO` dias.
- **Cópia externa (obrigatória):** o volume fica na mesma VPS, então copie para fora todo dia. Exemplo com `rclone` num cron do host: `docker run --rm -v dietamais_backups:/b:ro -v ~/.config/rclone:/config/rclone rclone/rclone copy /b remoto:dietamais-backups --max-age 2d`. Os arquivos já saem criptografados.
- **Teste de restauração (faça todo mês e depois de mudar o schema):** `docker compose -f compose.prod.yaml exec backup /bin/sh /ops/testar-restauracao.sh`. Restaura o backup mais recente num banco temporário, confere as tabelas-chave e as migrações, apaga o temporário e sai com erro se algo faltar. Para testar uma cópia vinda de fora, passe o caminho do arquivo como argumento.
- Arquivos gerados (PDF, logo): ficam no volume `arquivos` e os PDFs são apagados sozinhos. Inclua o volume no backup do host se usar a logo; com S3/MinIO (`S3_ENDPOINT`) o bucket tem a própria política de ciclo de vida (apagar `pdf/` e `exportacao/` com mais de 1 dia).

## Rotina de segurança

Chaves e senhas rotacionam trocando o valor no `.env.prod` e repetindo o `up -d`: `JWT_CHAVE_*` desloga todo mundo (aceitável), `ARQUIVOS_SEGREDO` invalida links em aberto, `BACKUP_SENHA` só vale para os próximos backups (guarde a antiga enquanto houver backup cifrado com ela). Veja também [seguranca.md](../arquitetura/seguranca.md).
