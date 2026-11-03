# BE-09 — catálogo base

Base develop c35541d, contrato BE-C2 aprovado. Migração seguinte: 010, sem editar 005 histórico (nome do plano estava desatualizado).

1. Referência local encontrada em pasta alternativa gitflow/doc-dieta. Extrair somente catálogo genérico das páginas de alimentos, nunca identificar pessoa/arquivo pessoal no Git ou carregar prontuário. Preservar subgrupos no nome para distinguir exemplos como Boi/Assada e Leites/Com café. CSV com 12 grupos/kcal e 482 alimentos/medida/gramas; nenhum valor de macro inferido.
2. Criar grupo_alimento/alimento e carga CSV, repositório/serviço de consulta global somente para nutri autenticada. Não implementar lista da nutri nem copiar catálogo em cadastro (BE-10), nem fontes sync (BE-14).
3. Testar dados, parser Liquibase e consulta/controle de perfil sem Docker; escrever integração HTTP/seed com contexto real e verificar identidade, catálogo global sem dado clínico e demais rotas ainda 501.
4. Decisão humana recebida em 27/10: pesquisar valores na internet e deixá-los editáveis posteriormente. Fonte TACO oficial/2011, normalização energética por representantes/proxy de sopa, uma casa decimal e rastreabilidade CSV/banco. Nenhum zero/nulo substitui ausência. Campo opcional macrosEstimados preserva nulabilidade/JSON anteriores. Método e limites em catalogo-base.md.
5. Concluir dados/API/docs/build/PR develop; QA exclusivo aprova/mergeia. BE-33 independente já mergeada em 20929ab e incorporada nesta branch. Edição funcional de cópias e UI ficam na BE-10, com caminho isolado/versionado documentado, sem afirmar essa implementação pronta na BE-09.
