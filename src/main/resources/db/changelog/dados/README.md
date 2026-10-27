# Origem do catálogo base

Dados genéricos transcritos da lista de substituição disponibilizada localmente pelo responsável do produto. Somente nomes de grupos/alimentos, kcal por porção, medidas caseiras e gramas foram extraídos. O PDF original e seus identificadores pessoais não integram o repositório. Subgrupos da tabela foram incorporados ao nome para evitar referências ambíguas, preservando abreviações da fonte.

`grupos-alimentos.csv`: 12 grupos com kcal confirmadas. `alimentos.csv`: 482 entradas com medidas de uma porção; IDs estáveis de seed e grupo referenciado. CSV UTF-8 separado por ponto e vírgula; decimais numéricos usam ponto, mantendo a escrita textual das medidas da fonte.

**Pendência BE-09:** fonte não informa os macros por porção dos grupos. Não foram inferidos a partir das kcal, preenchidos com zero ou copiados de dados pessoais/da dieta de referência. Contrato BE-C2 continua exigindo os três macros; carga final e ativação da consulta dependem de fonte autoritativa ou decisão explícita do produto. Nenhuma aprovação clínica de valores foi presumida.
