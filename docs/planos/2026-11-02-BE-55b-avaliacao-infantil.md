# BE-55b — integrar a avaliação infantil e adolescente

Branch: `feature/BE-55b-avaliacao-infantil`, base `origin/develop`, após o merge QA de MOB-61 (#66). Uma tarefa funcional por PR; revisão e merge pertencem ao QA.

## Resultado esperado

As avaliações de menores de 18 anos recebem entradas complementares opcionais e resultados calculados pelo servidor. Os resultados, os cortes efetivamente usados e a origem ficam congelados na gravação e acompanham REST e sync, respeitando o compartilhamento de seções. Clientes anteriores continuam funcionando; adultos mantêm suas calculadoras.

## Decisões e limites da fonte

- Usar meses completos na data local da avaliação (America/Sao_Paulo) para selecionar curvas e anos completos para Frisancho. Testar aniversários de 10 e 18 anos e nascimento futuro.
- As tabelas OMS contêm cortes, sem parâmetros LMS: devolver intervalo de percentil/Z delimitado pelos cortes disponíveis, sem interpolar ou apresentar um valor exato inventado. Igualdade com um corte pode produzir intervalo pontual. Congelar cortes e idade da linha, inclusive quando anual.
- G-16: AMBc masculino aos quatro anos tem p5 maior que p10. Preservar a referência e omitir a classificação afetada com motivo explícito. Não ordenar cortes para mascarar o problema.
- Frisancho contém CB, DCT e DCSE medianos; não comparar CMB com referência de CB. Conferir rótulos e fórmulas nas planilhas originais antes de fechar o contrato.
- Usar as calculadoras puras BE-55a, sem duplicar fórmulas nos clientes. A ausência de sexo, medida, atividade ou referência deve resultar em motivo, sem impedir os demais cálculos válidos.
- Novos campos são opcionais. PUT de cliente antigo preserva as entradas pediátricas existentes. Não aceitar derivados calculados pelo cliente.
- Entradas complementares ficam na visão da nutri. Resultados compartilhados respeitam medidas, composição e necessidades; motivos internos não vazam ao paciente.
- Changeset novo 048; não alterar referências/migrações já publicadas. Não editar .github nem fazer merge/review do próprio PR.

## Execução e verificação

1. Testar e implementar a leitura de cortes: limites, igualdade, intervalo aberto, tabela ausente/inválida, classes e escolha percentil/Z. Testes puros devem falhar antes da implementação.
2. Conferir fórmulas e unidades da fonte; definir DTOs aditivos e orquestrador infantil. Cobrir idades-limite, falta de dados, G-16 e vetores existentes. Validar limites pediátricos pela idade da ficha, nunca pelo corpo.
3. Integrar gravação/leitura com migration 048 e JSONB opcionais; manter construtores compatíveis. Testar criação, edição, leitura histórica, REST/sync, isolamento e filtragem por seção.
4. Documentar contrato, regras e limites da fonte; corrigir o parágrafo BE-57b que interrompe a tabela de módulos em integracao.md. Espelhar os docs comuns no mobile em branch própria.
5. Executar testes pertinentes e build completo; registrar separadamente eventual problema local conhecido de CSV CRLF. Atualizar plano de ação e PR template, abrir PR para develop e aguardar CI na cabeça atual antes de sinalizar ao QA.

## Progresso

- Pré-condições conferidas: nenhum PR aberto nos dois repositórios; MOB-61 mesclado após QA aprovado e checks verdes.
- Inspecionados orquestrador atual, DTOs, store, visibilidade e APIs OMS/Frisancho. A integração infantil ainda retorna FAIXA_ETARIA_NAO_SUPORTADA.
- Conferência somente de rótulos/fórmulas das planilhas originais: `0 a 9 anos - avaliação completa.xlsm`, aba `av. antropométrica`, B58 = perímetro do braço; E61 divide CB pela mediana de CB. A função `testa_cmb` é um nome legado; o dado é CB. AMBc/AGB mantêm a unidade numérica mm² das fórmulas e referências existentes, embora o rótulo da planilha diga cm². Não mudar a escala silenciosamente.
- Núcleos implementados: `CortesCrescimento`, `CalculoCurvasInfantis`, `CalculoFrisanchoAvaliacao`, com DTOs aditivos ainda não ligados à resposta pública da avaliação. Conservam cortes e origem, retornam intervalos, classes por protocolo e motivo G-16.
- TDD: cada classe nova foi precedida por teste com falha de símbolo ausente, registrada em `build/be-55b-{cortes,curvas,frisancho}-red.log`. A primeira execução do teste de seleção de protocolo revelou que o fixture havia repetido os mesmos limites clínicos nas duas listas; corrigido apenas o fixture para usar cortes diferentes, mantendo as expectativas e a calculadora existente.
- Verificação de 02/11: `./gradlew test --tests "*CalculoFrisanchoAvaliacaoTest" --tests "*CalculoCurvasInfantisTest" --tests "*CortesCrescimentoTest" --tests "*CalculadoraInfantilTest" --max-workers=2` passou (16 testes, zero falhas; `build/be-55b-referencias-green.log`). Não é build completo nem aprovação QA.
- Orquestrador e limites integrados pela idade da ficha, com aniversários completos; DTOs opcionais, edição legada preservando entradas, JSONB e migration 048 implementados. Migration amplia a precisão de estatura/IMC e mantém limites adultos nos CHECKs. Visibilidade filtra as mesmas seções no REST e sync.
- Testes de API viram falha antes da integração (lactente rejeitado pelos limites adultos, resposta sem resultados pediátricos); o fixture de data da API foi ajustado para ontem, evitando avaliação futura. Bateria final em `build/be-55b-integracao-final.log`: **35 testes, zero falhas**, incluindo escrita/leitura no sync, isolamento, snapshot sem recalcular e seções ocultas. Aniversário de 18 anos e integração Slaughter também cobertos.
- Documentados contrato aditivo e RN-CAL-08/RN-AVA-03/RN-CAL-02. Corrigida a interrupção da tabela de módulos por BE-57b. Espelho dos três documentos preparado no mobile, branch `feature/BE-55b-contrato-avaliacao-infantil`, ainda sem commit/PR.
- Build completo iniciado: `./gradlew build --max-workers=2`, log `build/be-55b-build-completo.log`. Aguardar e ler resultado antes de commit/PR. Se reaparecerem os quatro testes conhecidos de CSV CRLF, tratar apenas recursos gerados e registrar separadamente.
- Durante o build completo, `CalculoEnergiaAvaliacaoTest` revelou uma expectativa antiga de adolescente não suportado. Atualizada para `USE_ENERGIA_INFANTIL`, mantendo GET adulto ausente e exigindo GEB no bloco pediátrico. O teste falhou antes da atualização. `ProtocolosNutriApiTest.cadaNutriTemOsSeusEPacienteNaoAcessa` também falhou; aguardar XML do build para diagnosticar a resposta real, sem alterar a regra ou relaxar a expectativa.
- Resultado do primeiro build completo: 622 testes, oito falhas (15m15s). Uma expectativa antiga já ajustada; quatro casos conhecidos de CSV CRLF; três casos de API (`ProtocolosNutriApiTest`, `DietasPacienteApiTest`, `PacienteApiTest`). O último falhou com HTTP 401 ao criar paciente com token recém-emitido, em fluxo que não usa avaliação infantil. Os outros dois não verificavam status antes de interpretar o JSON; adicionadas asserções de status 200 com corpo sintético no diagnóstico, sem relaxar expectativas. Nenhum código de autenticação alterado.
- Reexecução diagnóstica dos quatro conjuntos não relacionados a CSV passou (3m19s), em `build/be-55b-regressoes.log`. As três falhas de API não reproduziram; a causa não foi determinada, portanto não atribuir a elas uma causa presumida nem esconder o primeiro resultado.
- Normalizados CRLF→LF somente em 15 CSV de `build/resources/main/db/changelog/dados`, sem alterar fontes versionadas. Novo build completo iniciado com `./gradlew build -x processResources --max-workers=2`, log `build/be-55b-build-final.log`, sessão 75995. O `-x` conserva os recursos gerados normalizados; todas as tarefas de build/teste continuam incluídas. Aguardar resultado antes de PR.
- Build final passou em 15m11s: **622 testes, 146 classes, zero falhas**, incluindo arquitetura, integrações e os quatro CSV; `build/be-55b-build-final.log`. Comando `./gradlew build -x processResources --max-workers=2`, com recursos gerados previamente normalizados. Fonte/migrações publicadas não alteradas para o problema CRLF. As falhas de API do primeiro build não repetiram neste build completo; causa permanece indeterminada.
- Próximo passo: commitar e abrir PRs develop para QA; CI na cabeça atual e aprovação/merge QA continuam obrigatórios. Não iniciar MOB-58 antes do merge QA.
- Implementação ainda em andamento; esta lista não constitui conclusão da tarefa.
