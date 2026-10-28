#!/bin/sh
# Teste de restauração (BE-32): restaura o backup mais recente (ou o arquivo passado) num banco temporário e confere as tabelas-chave.
# Roda no serviço de backup, que já tem o cliente do Postgres e a senha:
#   docker compose -f compose.prod.yaml exec backup /ops/testar-restauracao.sh [/backups/ARQUIVO.sql.gz.enc]
# Sai com código diferente de zero se a restauração falhar ou o banco restaurado vier vazio. Não toca no banco de produção.
set -eu
: "${BACKUP_SENHA:?defina BACKUP_SENHA}"
DIR="${BACKUP_DIR:-/backups}"
arquivo="${1:-$(ls -1t "$DIR"/dietamais-*.sql.gz.enc 2>/dev/null | head -n 1)}"
[ -n "$arquivo" ] && [ -f "$arquivo" ] || { echo "nenhum backup encontrado" >&2; exit 2; }
temporario="restauracao_teste_$(date -u +%Y%m%d%H%M%S)"
psql -d postgres -v ON_ERROR_STOP=1 -q -c "create database $temporario"
limpar() { psql -d postgres -q -c "drop database if exists $temporario with (force)" >/dev/null 2>&1 || true; }
trap limpar EXIT
openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_SENHA -in "$arquivo" | gunzip | psql -d "$temporario" -v ON_ERROR_STOP=1 -q >/dev/null
for tabela in databasechangelog usuario nutricionista assinatura plano; do
	existe=$(psql -d "$temporario" -At -c "select to_regclass('public.$tabela') is not null")
	[ "$existe" = "t" ] || { echo "tabela ausente na restauração: $tabela" >&2; exit 3; }
done
migracoes=$(psql -d "$temporario" -At -c "select count(*) from databasechangelog")
planos=$(psql -d "$temporario" -At -c "select count(*) from plano")
[ "$migracoes" -gt 0 ] && [ "$planos" -gt 0 ] || { echo "restauração veio vazia (migracoes=$migracoes planos=$planos)" >&2; exit 4; }
echo "restauração ok: $arquivo (migrações=$migracoes, planos=$planos)"
