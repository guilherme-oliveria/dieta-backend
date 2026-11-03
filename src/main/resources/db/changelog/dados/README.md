# Origem do catálogo base

Dados genéricos transcritos da lista de substituição disponibilizada localmente pelo responsável do produto. Somente nomes de grupos/alimentos, kcal por porção, medidas caseiras e gramas foram extraídos. O PDF original e seus identificadores pessoais não integram o repositório. Subgrupos da tabela foram incorporados ao nome para evitar referências ambíguas, preservando abreviações da fonte.

`grupos-alimentos.csv`: 12 grupos com kcal confirmadas. `alimentos.csv`: 482 entradas com medidas de uma porção; IDs estáveis de seed e grupo referenciado. CSV UTF-8 separado por ponto e vírgula; decimais numéricos usam ponto, mantendo a escrita textual das medidas da fonte.

Decisão humana em 27/10/2026 liberou pesquisa na internet e edição posterior. `fontes-macros-grupos.csv` registra representantes da TACO/NEPA-UNICAMP 4ª edição/2011, URL, páginas, data, valores-base e método. Os macros de grupo são estimativas normalizadas por energia, não composição medida de todos os alimentos. [Método, limites e caminho de edição isolada em BE-10](../../../../../../docs/arquitetura/catalogo-base.md). Nenhum dado desconhecido foi preenchido como zero/nulo e nenhuma validação clínica foi presumida.
