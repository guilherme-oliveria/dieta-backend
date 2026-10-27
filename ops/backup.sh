#!/bin/sh
# Backup diário do Postgres (BE-32a): pg_dump comprimido em /backups, mantém os últimos DIAS_RETENCAO dias.
# Variáveis: PGHOST PGUSER PGPASSWORD PGDATABASE (padrão do cliente), DIAS_RETENCAO (padrão 14), HORA_BACKUP (HH:MM UTC, padrão 03:00).
set -eu
DIAS_RETENCAO="${DIAS_RETENCAO:-14}"
HORA_BACKUP="${HORA_BACKUP:-03:00}"
fazer_backup() {
	arquivo="/backups/dietamais-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
	pg_dump --no-owner --clean --if-exists | gzip -9 > "$arquivo.tmp"
	mv "$arquivo.tmp" "$arquivo"
	find /backups -name 'dietamais-*.sql.gz' -mtime "+$DIAS_RETENCAO" -delete
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
