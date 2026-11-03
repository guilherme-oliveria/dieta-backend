#!/usr/bin/env bash
# Valida o backend como o CI faz, no WSL do Guilherme (precisa de Docker para o Testcontainers).
# Uso: ops/validar-wsl.sh   ->  resultado em build/validacao-wsl.txt (e no terminal)
set -uo pipefail
cd "$(dirname "$0")/.."
saida=build/validacao-wsl.txt
mkdir -p build
if ! docker info >/dev/null 2>&1; then echo "Docker não está rodando: abra o Docker Desktop (integração com o WSL ligada)." | tee "$saida"; exit 2; fi
{
	echo "commit: $(git rev-parse --short HEAD) ($(git rev-parse --abbrev-ref HEAD))"
	echo "início: $(date -Is)"
} | tee "$saida"
./gradlew build --max-workers=2 2>&1 | tee -a build/validacao-wsl.log | tail -n 25
codigo=${PIPESTATUS[0]}
{
	echo "fim: $(date -Is)"
	[ "$codigo" -eq 0 ] && echo "RESULTADO: OK" || echo "RESULTADO: FALHOU (código $codigo; log em build/validacao-wsl.log)"
} | tee -a "$saida"
exit "$codigo"
