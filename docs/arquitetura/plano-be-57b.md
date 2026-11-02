# BE-57b — Anamnese estruturada

Execução autorizada após MOB-55, aprovado e mesclado pelo QA no PR mobile #64.

- [x] Contrato aditivo: `codigo` opcional por pergunta; `derivados` opcionais na resposta, somente calculados pelo servidor.
- [x] Testes de códigos, tipos, limites, dados incompletos, condição oculta e vetores existentes (PA, álcool, sal, óleo e duração).
- [x] Changeset 047: código persistido, resultados congelados e identificação interna do modelo completo para criação única sem substituir modelos antigos/editados.
- [x] Modelo “Anamnese completa” em 12 blocos, com clínica, comportamento, alimentar e os 20 grupos efetivamente presentes nas três planilhas; perguntas estruturadas opcionais.
- [x] Mesmo cálculo na gravação parcial REST/sync e na conclusão; dados ausentes não viram zero. Resultado concluído permanece imutável.
- [x] Testes de API e isolamento, congelamento, edição/exclusão parcial, sync e compatibilidade com questionários antigos.
- [ ] Build completo, docs e contrato de exemplo; espelhar docs comuns no mobile por PR de contrato.
- [ ] PR para develop; aguardar QA independente antes da próxima tarefa.

Decisão de integração: códigos independem do texto e são únicos por modelo; apenas códigos conhecidos geram cálculos. O modelo novo é identificado por origem interna, para que renomear/editar não provoque duplicação. Em contas antigas a cópia anterior é preservada. Nenhuma migração publicada é alterada. Valores seguem CalculadoraAnamnese/contrato/calculos/anamnese.json (incluindo truncamento de sal e óleo), mesmo quando o exemplo visual diverge.

Fonte localizada pelo Guilherme em `Documento dieta` e consultada em modo de leitura. As três planilhas de avaliação têm 20 grupos e sete frequências em `av. alimentar!A92:H112`, em vez da estimativa de 30. Oito bebidas e atividades semanais incluídas; nenhuma resposta ou dado pessoal foi copiado. Estrutura e adaptações documentadas em `contrato-anamnese.md`.
