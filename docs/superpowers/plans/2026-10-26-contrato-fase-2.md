# BE-C2 — contrato da fase 2

Base: develop 014ef6f, BE-08 aprovada. Uma tarefa e PR, sem persistência/migração/implementação de regras da fase 2.

1. Publicar records tipados para catálogo/lista da nutri/ajustes por paciente, dieta inteira e snapshot imutável, seleção da dieta em uso e perfil BE-33 (incluído na fase 2 do plano).
2. Publicar 16 operações OpenAPI com sucesso previsto e ProblemDetail, todas retornando 501. Perfil/dono são autenticados; requisições não escolhem nutri. Versionamento/If-Match distinguem entidade da revisão do sync; totais vêm do backend.
3. Manifesto separado fase-2 e exemplos fictícios por operação, mantendo intactos os 49 exemplos fase 1. Testar roundtrip estrito, validação aninhada, erros/RNs e cobertura HTTP/OpenAPI/Bearer.
4. Documentar unidades, paginação, diferenças da lista, snapshots, versão/idempotência e limites de implementação. Reutilizar DIETA_VAZIA/RN-DIE-06 e DIETA_NAO_EDITAVEL/RN-DIE-04 já existentes; não inventar regra para gerar enum novo.
5. Executar testes focados e build; registrar limitação Docker/CI quota. Abrir PR develop e ceder janela para QA. Somente após gate avançar próxima tarefa.
