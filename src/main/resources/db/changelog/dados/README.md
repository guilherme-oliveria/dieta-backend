# Origem do catálogo base

Dados genéricos transcritos da lista de substituição disponibilizada localmente pelo responsável do produto. Somente nomes de grupos/alimentos, kcal por porção, medidas caseiras e gramas foram extraídos. O PDF original e seus identificadores pessoais não integram o repositório. Subgrupos da tabela foram incorporados ao nome para evitar referências ambíguas, preservando abreviações da fonte.

`grupos-alimentos.csv`: 12 grupos com kcal confirmadas. `alimentos.csv`: 482 entradas com medidas de uma porção; IDs estáveis de seed e grupo referenciado. CSV UTF-8 separado por ponto e vírgula; decimais numéricos usam ponto, mantendo a escrita textual das medidas da fonte.

Decisão humana em 27/10/2026 liberou pesquisa na internet e edição posterior. `fontes-macros-grupos.csv` registra representantes da TACO/NEPA-UNICAMP 4ª edição/2011, URL, páginas, data, valores-base e método. Os macros de grupo são estimativas normalizadas por energia, não composição medida de todos os alimentos. [Método, limites e caminho de edição isolada em BE-10](../../../../../../docs/arquitetura/catalogo-base.md). Nenhum dado desconhecido foi preenchido como zero/nulo e nenhuma validação clínica foi presumida.

## referencia-taco.csv (BE-47a)

597 alimentos da **Tabela Brasileira de Composição de Alimentos (TACO), 4ª edição revista e ampliada, NEPA-UNICAMP, 2011**, por 100 g de parte comestível. Fonte: PDF oficial (https://nepa.unicamp.br/wp-content/uploads/sites/27/2023/10/taco_4_edicao_ampliada_e_revisada.pdf). A obra diz: "É permitida a reprodução parcial ou total desta obra, desde que citada a fonte." Por isso o app e os PDFs devem citar a TACO (`fonte` das respostas da API).
Colunas: tabela 1 (centesimal, minerais e vitaminas) e, da tabela 2, os ácidos graxos saturados, mono e poliinsaturados. Vazio = não analisado (NA na fonte); `Tr` (traço) virou 0 e o nutriente fica listado em `traco`. Valores marcados com nota de rodapé na fonte (letra após o número) foram gravados sem a letra. Extraído do PDF por script de posição das colunas, conferido por consistência de energia (4P + 9L + 4C): fora da tolerância só aguardente, cerveja e fermento (álcool e NA).
