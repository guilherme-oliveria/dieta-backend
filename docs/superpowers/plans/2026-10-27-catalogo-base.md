# BE-09 — catálogo base

Base develop c35541d, contrato BE-C2 aprovado. Migração seguinte: 010, sem editar 005 histórico (nome do plano estava desatualizado).

1. Referência local encontrada em pasta alternativa gitflow/doc-dieta. Extrair somente catálogo genérico das páginas de alimentos, nunca identificar pessoa/arquivo pessoal no Git ou carregar prontuário. Preservar subgrupos no nome para distinguir exemplos como Boi/Assada e Leites/Com café. CSV com 12 grupos/kcal e 482 alimentos/medida/gramas; nenhum valor de macro inferido.
2. Criar grupo_alimento/alimento e carga CSV, repositório/serviço de consulta global somente para nutri autenticada. Não implementar lista da nutri nem copiar catálogo em cadastro (BE-10), nem fontes sync (BE-14).
3. Testar dados, parser Liquibase e consulta/controle de perfil sem Docker; escrever integração HTTP/seed com contexto real e verificar identidade, catálogo global sem dado clínico e demais rotas ainda 501.
4. Antes de tornar GET /grupos-alimentos real, resolver macros ausentes na fonte. RN-ALI-02/PDF fornecem kcal; RN-DIE-05 e DTO C2 aprovado exigem carboidratos/proteínas/gorduras por porção. Não usar zero como desconhecido nem alterar nulabilidade do contrato sem decisão humana. Coordenador informado e produto consultado; continuar partes independentes.
5. Após fonte/decisão, concluir dados/API/docs/build/PR develop. QA exclusivo aprova/mergeia. Se persistir a dependência de produto, preservar branch e executar BE-33 independente em outra branch da develop, sem misturar PRs.
