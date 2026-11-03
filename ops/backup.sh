#!/bin/sh
# Backup diário do Postgres (BE-32): pg_dump comprimido e criptografado (AES-256) em /backups, mantém os últimos DIAS_RETENCAO dias.
# Variáveis: PGHOST PGUSER PGPASSWORD PGDATABASE (padrão do cliente), BACKUP_SENHA (obrigatória), DIAS_RETENCAO (padrão 14), HORA_BACKUP (HH:MM UTC, padrão 03:00).
# Restaurar: openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_SENHA -in ARQUIVO.sql.gz.enc | gunzip | psql ...  (ou ops/testar-restauracao.sh)
set -eu
: "${BACKUP_SENHA:?defina BACKUP_SENHA}"
DIR="${BACKUP_DIR:-/backups}"
DIAS_RETENCAO="${DIAS_RETENCAO:-14}"
HORA_BACKUP="${HORA_BACKUP:-03:00}"
fazer_backup() {
	arquivo="$DIR/dietamais-$(date -u +%Y%m%dT%H%M%SZ).sql.gz.enc"
	pg_dump --no-owner --clean --if-exists | gzip -9 | openssl enc -aes-256-cbc -pbkdf2 -salt -pass env:BACKUP_SENHA > "$arquivo.tmp"
	mv "$arquivo.tmp" "$arquivo"
	find "$DIR" -name 'dietamais-*.sql.gz*' -mtime "+$DIAS_RETENCAO" -delete
	echo "backup ok: $arquivo"
}
if [ "${1:-}" = "agora" ]; then fazer_backup; exit 0; fi
while true; do
	agora=$(date -u +%s)
	alvo=$(date -u -d "$(date -u +%Y-%m-%d) $HORA_BACKUP:00" +%s)
	[ "$alvo" -le "$agora" ] && alvo=$((alvo + 86400))
	sleep $((alvo - agora))
	fazer_backup || echo "backup FALHOU" >&2
done
