# BE-50b — execução do PDF da lista de substituição

Especificação: `handoff-codex.md`, BE-50b, e artboards `PDF-Lista-Pequena`, `PDF-Lista-Media`, `PDF-Lista-Grande` e `PDF-Lista-Carboidratos` do mobile. Execução contínua autorizada em `plano-de-execucao.md`.

O módulo `relatorios` reutiliza a consulta autorizada do snapshot e o armazenamento temporário do PDF da dieta. A montagem da lista fica separada do gerador geral, sem acesso a banco. Somente APIs dos módulos `alimentos` e `referencias` fornecem a referência nutricional. Nomes, gramas e medidas vêm da lista congelada; valores de energia e carboidrato são médias provisórias por grupo e levam a fonte.

- [ ] Escrever e executar testes de escala das medidas (frações, decimais e texto sem número), conteúdo dos tamanhos e dados ausentes.
- [ ] Implementar escala arredondada para ¼, seleção por kcal crescente e tabela por grupo; não inventar dados ausentes.
- [ ] Expor as duas rotas anotadas no OpenAPI, reutilizando autorização, auditoria e links de 10 minutos.
- [ ] Cobrir acesso da nutri, paciente e isolamento em `PdfApiTest`; conferir PDF renderizado e paginação.
- [ ] Atualizar contrato, changelog e tarefa; executar build completo; abrir PR para `develop` para QA independente.

Conferir especialmente: lista vazia; grupo sem opção; alimento removido da referência depois da publicação; texto com caracteres XHTML; grupo grande que atravessa páginas. As quatro versões usam A4, duas colunas de grupos, marca D+, CRN e rodapé. Carboidratos usa a porção média e acrescenta o exemplo de soma por refeição.
