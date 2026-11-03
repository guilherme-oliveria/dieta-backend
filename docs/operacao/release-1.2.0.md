# Candidata 1.2.0 — M8

Preparação autorizada pelo pedido das ondas 20 a 23. Reutiliza o corte histórico `release/1.2.0` (5d894e5), criado antes das tarefas M9/M10; não mistura essas funcionalidades na candidata M8 nem reescreve a branch existente.

Versão Gradle alinhada em 1.2.0. Changelog agrupa referências e cálculos antropométricos do adulto, avaliação ampliada e protocolos. Não representa implantação nem liberação dos cadastros bloqueados por termos.

Validação desta candidata concluída com `./gradlew build --max-workers=2` e Testcontainers no WSL, em 14m11s (`build/release-1.2.0-validacao.log`). O CI da develop atual não substitui a verificação deste corte histórico. Não foi necessário normalizar CSVs neste corte.

PR será destinado à main para revisão do QA e aprovação/merge exclusivamente pelo Guilherme. Nenhuma tag manual ou alteração de workflow. Termos, credenciais, VPS, gateway, lojas e decisões clínicas G-15/G-16 continuam pendentes conforme os documentos vigentes da develop.

## Triagem dos checks de segurança

Correção solicitada pelo Guilherme após os checks do PR #75. O Gitleaks identificou como `generic-api-key` uma instrução SQL parametrizada em `CalendarioStore.java:17`, commit histórico `48a77ffec3ee505f476b30ef6dbf9158f54d85ce`. Não há credencial literal nessa linha; `.gitleaksignore` registra somente o fingerprint desse achado, mantendo a regra e futuras ocorrências ativas.

O alerta CodeQL #2 (`java/insufficient-key-size`) refere-se à chave de 1024 bits criada exclusivamente no teste negativo que exige a rejeição de chaves fracas. Foi classificado como `used in tests`, sem remover o teste nem alterar o mínimo de 2048 bits aceito. Nenhuma alteração em `.github/` ou reescrita de histórico.
