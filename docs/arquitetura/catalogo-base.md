# Catálogo base — BE-09

GET /api/v1/grupos-alimentos é real, autenticado e exclusivo do perfil NUTRICIONISTA. Devolve 12 grupos globais, sem dados clínicos ou identificadores de outros donos, ordenados por nome/id e com no-store. Assinatura inativa mantém a leitura. A lista da nutri, ajustes do paciente e suas escritas continuam em BE-10; esta entrega não permite a uma nutri editar o catálogo global.

## Dados e rastreabilidade

O catálogo genérico contém 12 grupos/kcal e 482 alimentos com medida caseira e gramas de uma porção, transcritos da lista de substituição local. Subgrupos foram incorporados ao nome para distinguir entradas como Boi/Assada e Leites/Com café. O PDF e qualquer identificação pessoal ficam fora do Git. As kcal, medidas e gramas do documento foram preservadas.

Por decisão explícita do Guilherme em 27/10/2026, os macros faltantes foram pesquisados na internet e ficam editáveis posteriormente. Fonte numérica: [TACO, 4ª edição revisada e ampliada, NEPA/UNICAMP, 2011](https://nepa.unicamp.br/wp-content/uploads/sites/27/2023/10/taco_4_edicao_ampliada_e_revisada.pdf), Tabela 1. Consulta em 27/10/2026. A [TBCA/USP](https://tbca.net.br/) também foi consultada; o registro específico de sopa encontrado no índice não estava acessível, portanto seus números não foram usados. Os dados numéricos abaixo são exclusivamente da TACO oficial.

`fontes-macros-grupos.csv` e a tabela `fonte_macros_grupo` registram URL, edição, data, número do alimento, páginas do PDF (contadas desde a capa, começando em 1), representante, quantidade-base, valores de origem, método e indicador de estimativa. Grupos, macros e proveniência são dados persistidos, não constantes no serviço.

| Grupo | kcal do PDF/porção | Referência TACO | Página PDF |
| --- | ---: | --- | ---: |
| Pão | 150 | 53 — Pão, trigo, francês | 31 |
| Recheio ou molho | 40 | 159 — Tomate, molho industrializado | 39 |
| Carne ou ovo | 200 | 488 — Ovo, de galinha, inteiro, cozido/10minutos | 59 |
| Arroz | 80 | 3 — Arroz, tipo 1, cozido | 29 |
| Feijão | 45 | 561 — Feijão, carioca, cozido | 65 |
| Leite | 55 | 459 — Leite, de vaca, integral, pó | 57 |
| Fruta | 70 | 182 — Banana, prata, crua | 39 |
| Vegetal refogado | 50 | 116 — Couve, manteiga, refogada | 35 |
| Castanha | 30 | 589 — Castanha-do-Brasil, crua | 67 |
| Gordura | 45 | 261 — Manteiga, com sal | 45 |
| Açúcar | 50 | 501 — Doce, de leite, cremoso | 61 |
| Sopa | 250 | Proxy composto 3 + 488 + 116 | 29 + 59 + 35 |

## Método da estimativa inicial

Os valores de origem dos representantes são por **100 g comestíveis**, não por porção do grupo. Para cada macro:

`macro_estimado_por_porção = macro_da_base × kcal_da_porção_PDF ÷ kcal_da_base`

Exemplo técnico: pão, base 300 kcal/100 g, carboidrato 58,6 g/100 g; porção do grupo com 150 kcal resulta em 29,3 g de carboidrato estimado. A quantidade-base consta na proveniência; a fórmula não substitui as gramas/medidas de nenhum alimento do PDF. Leite em pó foi escolhido por ter valores numéricos completos; não é uma instrução para consumir aquela quantidade de pó.

Para Sopa, o proxy soma **100 g de arroz cozido + 100 g de ovo cozido + 100 g de couve refogada**, usando somente as três referências acima. A base composta de 300 g tem 364 kcal, 37,4 g C, 17,5 g P e 16,3 g G. Normalizar a base para as 250 kcal do grupo resulta em 25,7 g C, 12,0 g P e 11,2 g G. Isso é uma **estimativa composta técnica**, não uma receita do PDF, receita recomendada ou valor medido de uma sopa real.

Arredondamento inicial: uma casa decimal em gramas, HALF_UP, somente ao final. Zero arredondado (por exemplo macro muito pequeno da manteiga) não significa dado ausente. Entradas `Tr`, `NA` ou `*` não foram convertidas em zero; escolheram-se representantes com os três valores numéricos disponíveis. Nenhum macro é nulo. A capacidade numeric(12,4) do banco não implica quatro casas de precisão científica. Os testes recalculam todas as estimativas e verificam a soma do proxy, unidades e arredondamento.

Os representantes são aproximações iniciais de grupos heterogêneos, **não médias analíticas de todos os seus alimentos**. Por exemplo, um grupo de molhos/recheios inclui preparos com proporções muito diferentes de gordura/carboidrato. O uso de um representante não torna cada alimento nutricionalmente equivalente em macros. Não se forçou igualdade das kcal com 4C+4P+9G; as kcal oficiais do PDF permanecem independentes, e a fonte/arredondamento podem produzir diferença. A nutri poderá revisar sua cópia; não foi presumida validação clínica dessas estimativas.

GET publica o campo novo opcional `macrosEstimados: true` para essas cargas. Ausência do campo em clientes/fixtures anteriores continua válida e não deve ser interpretada como garantia de valor medido. Os campos de nutrientes existentes mantêm nome/tipo/nulabilidade.

## Edição posterior funcional e isolamento — BE-10

O caminho previsto não se resume a edição manual do banco. BE-10 deve criar `grupo_alimento_nutri`, chave `(nutricionista_id, grupo_alimento_id)`, copiando os macros e sua proveniência/revisão ao inicializar a lista da nutri. O ID público do grupo é mantido para as referências dos alimentos; o dono da cópia vem exclusivamente da sessão. Cadastros existentes recebem a cópia uma vez, sem recriação a cada leitura.

Extensão funcional prevista em BE-10: `PATCH /grupos-alimentos/{id}`, corpo com `carboidratosPorPorcao`, `proteinasPorPorcao` e `gordurasPorPorcao`, todos números não negativos, e `If-Match` obrigatório. O GET passará a retornar a cópia própria com campo novo opcional `versao`; PATCH devolverá a mesma projeção e ETag. O editor de grupo no app da nutri permitirá ajustar os três campos e salvar sua cópia. Esta rota/DTO/editor são da próxima tarefa, **não estão disponíveis na BE-09**; os contratos concretos serão adicionados compativelmente antes da integração da UI.

A escrita BE-10 terá guarda de assinatura, versão, auditoria de configuração, 404 para referência fora da cópia autorizada e transação única para macros/proveniência. Origem manual será diferenciada da estimativa inicial. Testes BE-10 devem provar que nutri A não altera cópia B, que conflito não sobrescreve e que atualização posterior do catálogo base não modifica cópias já existentes (RN-ALI-01/04). Não haverá propagação global por UPDATE do grupo base nem fallback dinâmico aos macros base depois da cópia.

Gestão do catálogo **base** é administrativa, distinta dessa edição isolada; não foi criada API de escrita global neste PR. Uma revisão administrativa futura deve versionar valores e origem, afetando somente cópias criadas depois ou atualização explicitamente aceita conforme RN-ALI-04/P2. BE-12 congelará macros, lista e orientação nos snapshots; ajustar a cópia da nutri não reescreverá dietas publicadas.

## Migração e verificações

010 é a próxima numeração reservada da BE-09, antes de 011/BE-33 já aprovada. É nova; nenhuma migração histórica foi alterada. Cria grupo_alimento, alimento e fonte_macros_grupo, restrições e carga CSV tipada. As três tabelas não têm dado pessoal. Fontes sync novas entram apenas em BE-14.

Testes sem banco validam os CSVs/referências, método, limites numéricos, perfil, HTTP/OpenAPI/contratos e parsing Liquibase. Testes de integração escritos verificam carga, leitura global sem dado clínico, identidade/perfil/assinatura e valores lidos do banco. Parsing não substitui execução SQL/Postgres; indisponibilidade de Docker/CI deve ser explicitada no PR.
