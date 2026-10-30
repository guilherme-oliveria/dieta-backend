#!/usr/bin/env bash
# Ensaio de produção sem VPS (A9): sobe o compose.prod.yaml de verdade (app + Postgres 18 + Caddy + backup) com DOMINIO=localhost,
# o Caddy usa a CA interna (certificado de teste), e confere readiness, web, API, logs JSON, 426, CORS, cookie do refresh, backup e restauração.
# Uso: ops/ensaio-producao-wsl.sh        (resultado em build/ensaio-producao.txt; roteiro em docs/operacao/ensaio-producao.md)
# Variáveis: DIETA_WEB_DIST (padrão ../dieta-mobile/webApp/build/dist/wasmJs/productionExecutable), ENSAIO_MANTER=1 (não derruba no fim).
# Usa um projeto Compose próprio (dietamais-ensaio) e segredos descartáveis num diretório temporário: nada vai para o Git nem toca no seu banco.
# Precisa das portas 80 e 443 livres no WSL.
set -uo pipefail
cd "$(dirname "$0")/.."
raiz=$PWD
mkdir -p build
saida=build/ensaio-producao.txt
dist=${DIETA_WEB_DIST:-$raiz/../dieta-mobile/webApp/build/dist/wasmJs/productionExecutable}
tmp=$(mktemp -d)
projeto=dietamais-ensaio
falhas=0
base=https://localhost
compose() { docker compose -p "$projeto" -f compose.prod.yaml -f "$tmp/override.yaml" --env-file "$tmp/.env" "$@"; }

registrar() { echo "$*" | tee -a "$saida"; }
confere() { # confere "descrição" comando...   (o comando devolve 0 quando está certo)
	local descricao=$1; shift
	if "$@" >"$tmp/ultima.txt" 2>&1; then registrar "ok      $descricao"; else registrar "FALHOU  $descricao"; sed 's/^/          /' "$tmp/ultima.txt" | head -5 | tee -a "$saida" >/dev/null; falhas=$((falhas + 1)); fi
}
limpar() {
	if [ -z "${ENSAIO_MANTER:-}" ]; then compose down -v >/dev/null 2>&1; rm -rf "$tmp"; else echo "mantido: projeto $projeto, segredos em $tmp"; fi
}
trap limpar EXIT

: > "$saida"
registrar "commit: $(git rev-parse --short HEAD) ($(git rev-parse --abbrev-ref HEAD))"
registrar "início: $(date -Is)"
for ferramenta in docker curl openssl awk; do command -v "$ferramenta" >/dev/null || { registrar "Falta '$ferramenta' no PATH"; exit 2; }; done
if ss -ltn 2>/dev/null | grep -qE ':(80|443)\s'; then registrar "As portas 80 ou 443 já estão em uso no WSL; libere-as e rode de novo."; exit 2; fi

# --- segredos descartáveis (.env fora do repositório) ---
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$tmp/jwt-privada.pem" 2>/dev/null
openssl rsa -pubout -in "$tmp/jwt-privada.pem" -out "$tmp/jwt-publica.pem" 2>/dev/null
senha_admin="Ensaio-$(openssl rand -hex 8)-Admin"
cat > "$tmp/.env" <<EOF
DOMINIO=localhost
DB_PASSWORD=$(openssl rand -hex 16)
JWT_CHAVE_PRIVADA="$(awk 'NF{printf "%s\\n",$0}' "$tmp/jwt-privada.pem")"
JWT_CHAVE_PUBLICA="$(awk 'NF{printf "%s\\n",$0}' "$tmp/jwt-publica.pem")"
IDEMPOTENCIA_CHAVE=$(openssl rand -base64 32)
NOTIFICACOES_CHAVE=$(openssl rand -base64 32)
ARQUIVOS_SEGREDO=$(openssl rand -base64 32)
BACKUP_SENHA=$(openssl rand -hex 16)
WEB_ORIGEM=https://localhost
CORS_ORIGENS=https://localhost,https://web.ensaio.test
APP_VERSAO_MINIMA=1.0.0
APP_VERSAO_MINIMA_WEB=
CONSENTIMENTO_VERSAO=ensaio-v1
CONSENTIMENTO_TEXTO=Texto de ensaio, sem valor juridico.
EMAIL_URL_CONVITE=https://localhost/convite
ADMIN_EMAIL=admin.ensaio@example.invalid
ADMIN_SENHA=$senha_admin
EOF
chmod 600 "$tmp/.env"
mkdir -p "$tmp/web"
if [ -d "$dist" ]; then cp -r "$dist"/. "$tmp/web"/; web_ok=1; else web_ok=0; registrar "AVISO: web não encontrada em $dist; as checagens da web serão puladas"; fi
# A web vem de fora do repositório: o override troca o volume ./web por uma cópia temporária.
cat > "$tmp/override.yaml" <<EOF
services:
  caddy:
    volumes:
      - ./ops/Caddyfile:/etc/caddy/Caddyfile:ro
      - $tmp/web:/srv/web:ro
      - caddy-data:/data
      - caddy-config:/config
EOF

registrar "== subida (docker compose up --build)"
compose up -d --build >build/ensaio-up.log 2>&1 || { registrar "FALHOU  compose up (build/ensaio-up.log)"; tail -15 build/ensaio-up.log | tee -a "$saida"; exit 1; }
for _ in $(seq 1 60); do
	[ "$(compose ps --format '{{.Service}} {{.Health}}' 2>/dev/null | grep -c healthy)" -ge 2 ] && break
	sleep 5
done
registrar "== serviços"
compose ps --format '{{.Service}}: {{.State}} {{.Health}}' | tee -a "$saida"

C=(curl -ks --max-time 20)
registrar "== readiness e API"
confere "app saudável (healthcheck do compose)" bash -c "compose() { docker compose -p $projeto -f compose.prod.yaml -f $tmp/override.yaml --env-file $tmp/.env \"\$@\"; }; compose ps --format '{{.Service}} {{.Health}}' | grep -q 'app healthy'"
confere "readiness responde UP por dentro (exec no contêiner)" bash -c "docker compose -p $projeto -f compose.prod.yaml -f $tmp/override.yaml --env-file $tmp/.env exec -T app bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf \"GET /actuator/health/readiness HTTP/1.0\r\n\r\n\" >&3 && grep -q UP <&3'"
readiness_pelo_caddy() { local r; r=$("${C[@]}" -w ' HTTP%{http_code} %{content_type}' "$base/actuator/health/readiness"); echo "$r"; [[ $r == *'"status":"UP"'* && $r == *HTTP200*application/* ]]; }
confere "readiness pelo Caddy devolve JSON UP (e não o index.html da web)" readiness_pelo_caddy
confere "GET /legal/terms-of-use pelo Caddy responde JSON 200" bash -c "[ \"\$(${C[*]} -o /dev/null -w '%{http_code}' $base/legal/terms-of-use)\" = 200 ]"

registrar "== web (arquivos estáticos pelo Caddy)"
if [ "$web_ok" = 1 ]; then
	confere "GET / devolve index.html (200, text/html)" bash -c "${C[*]} -D - -o /dev/null $base/ | tr -d '\r' | grep -qi '^content-type: text/html'"
	confere "GET /convite/ABCD2345 cai no index.html (try_files)" bash -c "${C[*]} $base/convite/ABCD2345 | grep -q '<title>Dieta+</title>'"
	wasm=$(cd "$tmp/web" && ls *.wasm | head -1)
	confere "o .wasm sai como application/wasm" bash -c "${C[*]} -I $base/$wasm | tr -d '\r' | grep -qi '^content-type: application/wasm'"
	confere "o .wasm vem comprimido (zstd ou gzip)" bash -c "${C[*]} -I -H 'Accept-Encoding: zstd, gzip' $base/$wasm | tr -d '\r' | grep -qiE '^content-encoding: (zstd|gzip)'"
	confere "Cache-Control: no-cache com ETag nos estáticos" bash -c "${C[*]} -I $base/ | tr -d '\r' | grep -qi '^cache-control: no-cache' && ${C[*]} -I $base/ | tr -d '\r' | grep -qi '^etag:'"
	confere "segunda requisição com If-None-Match devolve 304" bash -c "e=\$(${C[*]} -I $base/dietamais.js | tr -d '\r' | awk -F': ' 'tolower(\$1)==\"etag\"{print \$2}'); [ \"\$(${C[*]} -o /dev/null -w '%{http_code}' -H \"If-None-Match: \$e\" $base/dietamais.js)\" = 304 ]"
	confere "CSP em modo só-relatório presente" bash -c "${C[*]} -I $base/ | tr -d '\r' | grep -qi '^content-security-policy-report-only:'"
	confere "HSTS presente" bash -c "${C[*]} -I $base/ | tr -d '\r' | grep -qi '^strict-transport-security:'"
fi

registrar "== 426 (versão mínima do app)"
confere "app antigo (X-App-Versao 0.9.0) recebe 426 com codigo VERSAO_APP_OBSOLETA" bash -c "r=\$(${C[*]} -H 'X-App-Versao: 0.9.0' -H 'X-Plataforma: ANDROID' -w ' HTTP%{http_code}' $base/api/v1/eu); echo \"\$r\"; echo \"\$r\" | grep -q 'HTTP426' && echo \"\$r\" | grep -q 'VERSAO_APP_OBSOLETA'"
confere "app atual (1.2.0) não é barrado pela versão" bash -c "[ \"\$(${C[*]} -o /dev/null -w '%{http_code}' -H 'X-App-Versao: 1.2.0' -H 'X-Plataforma: ANDROID' $base/api/v1/eu)\" != 426 ]"
confere "web (X-Plataforma: WEB) não é barrada pela mínima dos apps" bash -c "[ \"\$(${C[*]} -o /dev/null -w '%{http_code}' -H 'X-App-Versao: 0.9.0' -H 'X-Plataforma: WEB' $base/api/v1/eu)\" != 426 ]"

registrar "== CORS"
pre() { "${C[@]}" -D - -o /dev/null -X OPTIONS "$base/api/v1/auth/login" -H "Origin: $1" -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: content-type,idempotency-key,x-plataforma,x-app-versao' | tr -d ''; }
cors_origem_do_site() { local r; r=$(pre https://web.ensaio.test); echo "$r"; [[ $(head -1 <<<"$r") == *" 200"* && ${r,,} == *"access-control-allow-origin: https://web.ensaio.test"* && ${r,,} == *"access-control-allow-credentials: true"* ]]; }
cors_libera_cabecalhos() { local r; r=$(pre https://web.ensaio.test); echo "$r"; local h; h=$(grep -i "^access-control-allow-headers" <<<"$r"); [[ ${h,,} == *x-plataforma* && ${h,,} == *idempotency-key* ]]; }
cors_mesma_origem() { local r; r=$(pre https://localhost); echo "$r"; [[ $(head -1 <<<"$r") == *" 200"* ]]; }
cors_origem_estranha() { local r; r=$(pre https://evil.example); echo "$r"; [[ ${r,,} != *"access-control-allow-origin"* ]]; }
confere "preflight de outra origem permitida (https://web.ensaio.test): Allow-Origin igual e credenciais" cors_origem_do_site
confere "preflight libera X-Plataforma e Idempotency-Key" cors_libera_cabecalhos
confere "preflight da própria origem do site (https://localhost): 200 sem precisar de CORS (mesma origem)" cors_mesma_origem
confere "preflight de origem estranha (https://evil.example) não recebe Allow-Origin" cors_origem_estranha
registrar "== login web e cookie do refresh"
disp="{\"id\":\"$(cat /proc/sys/kernel/random/uuid)\",\"plataforma\":\"WEB\",\"nomeAparelho\":\"Ensaio\"}"
login=$("${C[@]}" -D "$tmp/cab-login.txt" -o "$tmp/login.json" -w '%{http_code}' -X POST "$base/api/v1/auth/login" -H 'Content-Type: application/json' -H "Idempotency-Key: $(cat /proc/sys/kernel/random/uuid)" -H 'Origin: https://localhost' -H 'X-Plataforma: WEB' -H 'X-App-Versao: 1.2.0' \
	-d "{\"email\":\"admin.ensaio@example.invalid\",\"senha\":\"$senha_admin\",\"dispositivo\":$disp,\"perfil\":null}")
confere "login do admin criado na primeira subida (HTTP 200)" test "$login" = 200
confere "cookie refreshToken é HttpOnly, Secure e SameSite=Strict, com Path restrito" bash -c "tr -d '\r' < $tmp/cab-login.txt | grep -i '^set-cookie: refreshToken=' | grep -qi 'httponly' && tr -d '\r' < $tmp/cab-login.txt | grep -i '^set-cookie: refreshToken=' | grep -qi 'secure' && tr -d '\r' < $tmp/cab-login.txt | grep -i '^set-cookie: refreshToken=' | grep -qi 'samesite=strict' && tr -d '\r' < $tmp/cab-login.txt | grep -i '^set-cookie: refreshToken=' | grep -qi 'path=/api/v1/auth'"
confere "o corpo do login web não traz o refreshToken (só no cookie)" bash -c "grep -q '\"refreshToken\":null' $tmp/login.json"
cookie=$(tr -d '\r' < "$tmp/cab-login.txt" | awk -F'[=;]' 'tolower($1)=="set-cookie: refreshtoken"{print $2}')
refresh() { "${C[@]}" -o /dev/null -w '%{http_code}' -X POST "$base/api/v1/auth/refresh" -H "Origin: $1" -H 'X-Plataforma: WEB' -H 'X-App-Versao: 1.2.0' -H "Idempotency-Key: $(cat /proc/sys/kernel/random/uuid)" -b "refreshToken=$cookie"; }
confere "refresh com o cookie e Origin do site: 200" test "$(refresh https://localhost)" = 200
confere "refresh com Origin estranha é recusado (não 200)" bash -c "[ \"\$(curl -ks -o /dev/null -w '%{http_code}' -X POST $base/api/v1/auth/refresh -H 'Origin: https://evil.example' -H 'X-Plataforma: WEB' -H 'X-App-Versao: 1.2.0' -H 'Idempotency-Key: $(cat /proc/sys/kernel/random/uuid)' -b 'refreshToken=$cookie')\" != 200 ]"

registrar "== logs em JSON"
compose logs app --no-log-prefix --tail 200 2>/dev/null > "$tmp/logs-app.txt"
total=$(grep -c . "$tmp/logs-app.txt"); json=$(grep -c '^{' "$tmp/logs-app.txt")
confere "todas as linhas de log do app são JSON ($json de $total)" test "$total" -gt 0 -a "$total" -eq "$json"
ecs() { local l; l=$(grep -m1 "^{" "$tmp/logs-app.txt"); [[ $l == *\"@timestamp\"* && $l == *\"level\"* && $l == *\"message\"* && $l == *\"ecs\"* ]]; }
confere "os logs seguem o formato ECS (@timestamp, log.level, message, ecs.version)" ecs
confere "o log não traz a senha do admin nem o refreshToken" bash -c "! grep -q '$senha_admin' $tmp/logs-app.txt && ! grep -q '$cookie' $tmp/logs-app.txt"

registrar "== backup e restauração"
confere "backup manual (ops/backup.sh agora) gera o arquivo criptografado" bash -c "docker compose -p $projeto -f compose.prod.yaml -f $tmp/override.yaml --env-file $tmp/.env exec -T backup /bin/sh /ops/backup.sh agora | grep -q 'backup ok'"
confere "o arquivo de backup não é texto legível (criptografado)" bash -c "docker compose -p $projeto -f compose.prod.yaml -f $tmp/override.yaml --env-file $tmp/.env exec -T backup sh -c 'f=\$(ls -1t /backups/dietamais-*.sql.gz.enc | head -1); head -c 8 \$f | grep -q Salted__'"
confere "teste de restauração (ops/testar-restauracao.sh) passa" bash -c "docker compose -p $projeto -f compose.prod.yaml -f $tmp/override.yaml --env-file $tmp/.env exec -T backup /bin/sh /ops/testar-restauracao.sh | tee /dev/stderr | grep -q 'restauração ok'"
confere "restauração com senha errada falha (não aceita lixo)" bash -c "! docker compose -p $projeto -f compose.prod.yaml -f $tmp/override.yaml --env-file $tmp/.env exec -T -e BACKUP_SENHA=senha-errada backup /bin/sh /ops/testar-restauracao.sh"

registrar "fim: $(date -Is)"
[ "$falhas" -eq 0 ] && registrar "RESULTADO: OK" || registrar "RESULTADO: FALHOU ($falhas checagens)"
exit "$falhas"
