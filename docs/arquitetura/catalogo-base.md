# Catálogo base — BE-09

GET /api/v1/grupos-alimentos é autenticado e exclusivo do perfil NUTRICIONISTA. BE-09 entregou a base inicial; após BE-10, devolve a cópia própria dos grupos, ordenada por nome/id e com no-store. Assinatura inativa mantém leitura. A edição de macros/alimentos e ajustes do paciente está implementada na API BE-10; nenhuma rota permite à nutri editar o catálogo global.

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

## Edição funcional isolada — BE-10

BE-10 cria grupo_alimento_nutri, chave (nutricionista_id,grupo_id), e alimento_nutri com IDs próprios/catalogoId. Valores e proveniência são copiados uma única vez no cadastro e no backfill dos existentes, com marcador lista_alimentos_nutri. Repetir a inicialização não recopia nem insere futuras alterações do catálogo. GET não inicializa/escreve. O ID público do grupo é mantido nas referências; o dono vem exclusivamente da sessão.

PATCH /grupos-alimentos/{id} está implementado: corpo de três macros não negativos, até quatro casas, If-Match obrigatório. GET da cópia acrescenta versao/origemMacros opcionais; PATCH devolve a projeção/ETag. O app pode integrar seu editor usando o contrato/exemplos oficiais BE-10; a UI pertence ao repositório mobile e não foi entregue neste PR backend. Nenhuma edição global/manual de banco é necessária para a nutri ajustar sua cópia pela API.

A escrita aplica guarda de assinatura no filtro/serviço, lock e versão; referência fora da cópia autorizada retorna 404. Origem inicial completa fica preservada em origem_base. Ajuste manual registra usuário/data, origem AJUSTE_NUTRI e histórico de configuração com valores/versões antes/depois, sem dado de paciente. macrosEstimados é omitido após ajuste manual, que não comprova medição laboratorial. Alteração e histórico são transacionais; conflito não escreve. Não há propagação para outra nutri nem fallback dinâmico ao grupo base.

Cada vínculo recebe baseline de IDs ativos da lista da nutri. Diferenças são persistidas por vínculo em ajuste_lista_paciente; alimento novo pode ser adicionado somente ao paciente escolhido. PUT substitui as diferenças e exige versão própria. Mudanças de configuração de alimento/grupo próprio revisam as versões das listas da mesma nutri por sequência dedicada; não usam a revisão do diário sync como If-Match. GET da projeção usa REPEATABLE_READ; escritas usam o advisory lock por nutri antes de locks de ficha/ajuste. ENCERRADO permite leitura e impede editar diferenças. Essa projeção usa os dados atuais da nutri; dietas publicadas terão o snapshot independente BE-12.

Gestão do catálogo **base** é administrativa, distinta dessa edição isolada; não foi criada API de escrita global neste PR. Uma revisão administrativa futura deve versionar valores e origem, afetando somente cópias criadas depois ou atualização explicitamente aceita conforme RN-ALI-04/P2. BE-12 congelará macros, lista e orientação nos snapshots; ajustar a cópia da nutri não reescreverá dietas publicadas.

## Migração e verificações

010 cria a base e proveniência; 012 acrescenta cópias, marcador, diferenças e histórico de configuração. Nenhuma migração histórica foi editada. A cópia inicial bloqueia fontes em SHARE para manter valores/proveniência consistentes até concluir o cadastro. Não há conteúdo clínico nas tabelas base; diferenças de paciente são restritas à ficha própria e auditadas somente por identificadores. Fontes sync novas entram em BE-14.

Testes sem banco validam os CSVs/referências, método, limites numéricos, perfil, HTTP/OpenAPI/contratos e parsing Liquibase. Testes de integração escritos verificam carga, leitura global sem dado clínico, identidade/perfil/assinatura e valores lidos do banco. Parsing não substitui execução SQL/Postgres; indisponibilidade de Docker/CI deve ser explicitada no PR.
