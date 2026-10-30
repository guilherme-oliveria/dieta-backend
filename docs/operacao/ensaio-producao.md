# Ensaio de produção local (A9)

Sobe o `compose.prod.yaml` de verdade, sem VPS e sem domínio, e confere o que a produção precisa fazer. Serve para não descobrir problemas só no dia da implantação (BE-32b).

## Como rodar

No WSL (Docker, `curl`, `openssl`; portas 80 e 443 livres), com a web já construída no `dieta-mobile` (`./gradlew :webApp:wasmJsBrowserDistribution`):

```bash
ops/ensaio-producao-wsl.sh        # resumo em build/ensaio-producao.txt; log da subida em build/ensaio-up.log
ENSAIO_MANTER=1 ops/ensaio-producao-wsl.sh   # deixa tudo no ar para investigar (projeto dietamais-ensaio)
```

O script usa `DOMINIO=localhost` (o Caddy emite o certificado pela CA interna, por isso os `curl -k`), um projeto Compose próprio (`dietamais-ensaio`) e segredos descartáveis num diretório temporário (JWT, chaves, `BACKUP_SENHA`, senha do admin): nada vai para o Git e nada toca no banco de desenvolvimento. A web vem de `../dieta-mobile/webApp/build/dist/wasmJs/productionExecutable` (ou `DIETA_WEB_DIST`) por um `override` do volume `./web`. No fim ele derruba tudo e apaga os volumes.

## O que é conferido

| Grupo | Checagens |
| --- | --- |
| Subida | app e Postgres `healthy`; readiness `UP` por dentro e **pelo Caddy** (JSON, não o `index.html`); `GET /legal/terms-of-use` |
| Web pelo Caddy | `/` devolve o `index.html`; `/convite/{codigo}` cai no `index.html` (`try_files`); `.wasm` como `application/wasm`; compressão (zstd ou gzip); `Cache-Control: no-cache` com ETag e `304` na segunda requisição; CSP só-relatório; HSTS |
| 426 | app antigo recebe `426` com `VERSAO_APP_OBSOLETA`; app atual não; a web (`X-Plataforma: WEB`) não é barrada pela mínima dos apps |
| CORS | outra origem permitida recebe `Allow-Origin` e credenciais; `X-Plataforma` e `Idempotency-Key` liberados; a própria origem do site passa sem precisar de CORS (mesma origem); origem estranha não recebe `Allow-Origin` |
| Sessão da web | login do admin criado na primeira subida; cookie `refreshToken` `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`; o corpo do login não traz o refresh; refresh com a `Origin` do site dá `200`, com origem estranha é recusado |
| Logs | todas as linhas em JSON no formato ECS (`@timestamp`, `log.level`, `message`, `ecs.version`); sem a senha do admin nem o refresh nos logs |
| Backup | `ops/backup.sh agora` gera o `.sql.gz.enc` criptografado (cabeçalho `Salted__`); `ops/testar-restauracao.sh` restaura num banco temporário e confere as tabelas; restaurar com a senha errada falha |

## Resultado (29/10/2026, WSL Ubuntu 24.04, imagem construída do zero)

31 checagens verdes na última rodada (`RESULTADO: OK`). A subida da imagem leva uns 4 minutos (Gradle dentro do Docker) e o resto, cerca de 30 segundos.

## O que o ensaio achou (e foi corrigido)

1. **A imagem de produção não subia.** O `.dockerignore` excluía `docs` e o `Dockerfile` não copiava `docs/legal`, mas o `build.gradle.kts` empacota `docs/legal` no JAR (`processResources`). Sem `terms-of-use.md`, o `FonteTermosUsoMarkdown` lançava exceção na criação do bean e o app caía na inicialização (`app is unhealthy`). Corrigido: `.dockerignore` com `docs/*` e `!docs/legal`, e `COPY docs/legal docs/legal` no estágio de build. **Qualquer implantação feita antes dessa correção falharia.**
2. **O readiness pelo Caddy devolvia a página da web.** O Caddy só encaminhava `/api` e `/legal`; `/actuator/health/readiness` caía no `try_files` e voltava `200 text/html` (o `index.html`). O `producao.md` manda conferir `curl https://$DOMINIO/actuator/health/readiness` esperando `{"status":"UP"}`, e um monitor externo apontado para lá veria "saudável" mesmo com o app fora. Corrigido: `handle /actuator/health/*` no `ops/Caddyfile` (o Actuator só expõe `health`, sem detalhes).

## O que o ensaio não cobre

- Certificado real e renovação (Let's Encrypt): aqui é a CA interna do Caddy. Na VPS, DNS e portas 80/443 abertas.
- Cópia externa do backup (`rclone` no cron do host) e o espaço em disco da VPS.
- E-mail, push e Asaas: ficam desligados sem as credenciais (`EMAIL_ENVIO_HABILITADO=false`, etc.).
- Carga e desempenho com dados reais.
- O link de convite precisa de `EMAIL_URL_CONVITE=https://…/convite` (o ensaio usa `https://localhost/convite`): com o padrão `dietamais://convites` o app recusa o link e a ficha mostra "Não foi possível concluir" (achado da A4, ver `dieta-mobile/docs/arquitetura/web.md`).

## Para a implantação real

Repita este roteiro apontando para a VPS trocando `-k` e `localhost` pelo domínio, depois do primeiro `up -d --build`, e confirme o primeiro backup e o teste de restauração antes de liberar para a nutri.
