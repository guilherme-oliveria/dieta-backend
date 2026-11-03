# Candidata 1.2.0 — M8

Preparação autorizada pelo pedido das ondas 20 a 23. Reutiliza o corte histórico `release/1.2.0` (5d894e5), criado antes das tarefas M9/M10; não mistura essas funcionalidades na candidata M8 nem reescreve a branch existente.

Versão Gradle alinhada em 1.2.0. Changelog agrupa referências e cálculos antropométricos do adulto, avaliação ampliada e protocolos. Não representa implantação nem liberação dos cadastros bloqueados por termos.

Validação desta candidata concluída com `./gradlew build --max-workers=2` e Testcontainers no WSL, em 14m11s (`build/release-1.2.0-validacao.log`). O CI da develop atual não substitui a verificação deste corte histórico. Não foi necessário normalizar CSVs neste corte.

PR será destinado à main para revisão do QA e aprovação/merge exclusivamente pelo Guilherme. Nenhuma tag manual ou alteração de workflow. Termos, credenciais, VPS, gateway, lojas e decisões clínicas G-15/G-16 continuam pendentes conforme os documentos vigentes da develop.
