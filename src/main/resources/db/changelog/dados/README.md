# Origem do catálogo base

Dados genéricos transcritos da lista de substituição disponibilizada localmente pelo responsável do produto. Somente nomes de grupos/alimentos, kcal por porção, medidas caseiras e gramas foram extraídos. O PDF original e seus identificadores pessoais não integram o repositório. Subgrupos da tabela foram incorporados ao nome para evitar referências ambíguas, preservando abreviações da fonte.

`grupos-alimentos.csv`: 12 grupos com kcal confirmadas. `alimentos.csv`: 482 entradas com medidas de uma porção; IDs estáveis de seed e grupo referenciado. CSV UTF-8 separado por ponto e vírgula; decimais numéricos usam ponto, mantendo a escrita textual das medidas da fonte.

Decisão humana em 27/10/2026 liberou pesquisa na internet e edição posterior. `fontes-macros-grupos.csv` registra representantes da TACO/NEPA-UNICAMP 4ª edição/2011, URL, páginas, data, valores-base e método. Os macros de grupo são estimativas normalizadas por energia, não composição medida de todos os alimentos. [Método, limites e caminho de edição isolada em BE-10](../../../../../../docs/arquitetura/catalogo-base.md). Nenhum dado desconhecido foi preenchido como zero/nulo e nenhuma validação clínica foi presumida.

## referencia-taco.csv (BE-47a)

597 alimentos da **Tabela Brasileira de Composição de Alimentos (TACO), 4ª edição revista e ampliada, NEPA-UNICAMP, 2011**, por 100 g de parte comestível. Fonte: PDF oficial (https://nepa.unicamp.br/wp-content/uploads/sites/27/2023/10/taco_4_edicao_ampliada_e_revisada.pdf). A obra diz: "É permitida a reprodução parcial ou total desta obra, desde que citada a fonte." Por isso o app e os PDFs devem citar a TACO (`fonte` das respostas da API).
Colunas: tabela 1 (centesimal, minerais e vitaminas) e, da tabela 2, os ácidos graxos saturados, mono e poliinsaturados. Vazio = não analisado (NA na fonte); `Tr` (traço) virou 0 e o nutriente fica listado em `traco`. Valores marcados com nota de rodapé na fonte (letra após o número) foram gravados sem a letra. Extraído do PDF por script de posição das colunas, conferido por consistência de energia (4P + 9L + 4C): fora da tolerância só aguardente, cerveja e fermento (álcool e NA).

## referencia-oms.csv (BE-54a)

Curvas da OMS (2006/2007) copiadas das abas **P.I.**, **E.I.** e **I.M.C.** da planilha "0 a 9 anos" (120 meses, de 0 a 119, por sexo) e da aba **I.M.C.** da planilha "10 a 17 anos" (uma linha por ano, de 10 a 17, que vale até o fim daquele ano). Cada linha traz os percentis 0,1, 3, 10, 15, 50, 85, 97 e 99 e os escores Z de -3 a +3. Os valores das curvas da OMS são públicos; a transcrição é a da planilha. Correção da planilha (G-16): de 5 a 10 anos (61 a 119 meses) as colunas 0,1° e 10° de peso e estatura variavam em degraus anuais e em 53 linhas o 3° percentil passava do 10°. Essas duas colunas foram refeitas a partir das tabelas oficiais da OMS (referência de 2007: estatura pela tabela de percentis; peso pelos parâmetros L, M e S, conferidos contra os percentis impressos com erro de até 0,05 kg) e o `OmsCsvTest` exige percentis crescentes em todas as linhas.

## referencia-exame.csv e referencia-exame-regra.csv (BE-56a)

Catálogo da aba **av. bioquimica** das três planilhas (0 a 9, 10 a 17 e 18 anos acima): 395 exames (131, 132 e 132) e 2.223 degraus, na ordem das fórmulas dos IF aninhados (o primeiro que serve vale). Os degraus foram extraídos das fórmulas por script e conferidos contra elas com um interpretador independente (mais de 160 mil combinações de valor, sexo, fase e idade, zero diferenças). Rótulos como na planilha (normal, alto, limite superior...). Exames qualitativos comparam o texto (ausente, não reagente...) e o resto cai em "alterado".
Inconsistência da planilha de 10 a 17 anos (G-16): a referência ao sexo (e à data de nascimento em alguns exames) virou `#REF!` em 30 fórmulas, então no Excel esses exames dão erro; tratamos `#REF!="feminino"` como o sexo e `#REF!<=12` como idade em anos. Na planilha de 0 a 9 a idade vem de `cadastro!$I$3` (idade de hoje); aqui vale a idade na data do exame.

## referencia-frisancho.csv (BE-54b)

Percentis de Frisancho (1981) copiados da aba **outras** da planilha de 0 a 9 anos: CB, DCT e DCSE (só a mediana), área muscular do braço corrigida (AMBC) e área de gordura do braço (AGB) com percentis 5, 10, 50 e 90, de 2 a 9 anos por sexo (80 linhas). A planilha não traz Frisancho abaixo de 2 anos nem para 10 a 17 anos. Inconsistência (G-16): AMBC masculino de 4 anos tem o percentil 5 (1297) acima do 10 (1264); mantido e coberto por `FrisanchoCsvTest`.
