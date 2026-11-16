#!/usr/bin/env python3
"""Extrai a aba Alimentos de "Dieta sem restrições.xlsm" para os CSVs da BE-47c.
Uso: python3 -I ops/dados/extrair-alimentos.py <planilha.xlsm> <pasta-dados-liquibase>
Lê alimentos.csv, referencia-taco.csv e grupos-alimentos*.csv da pasta e gera alimento-planilha.csv (alimentos novos do catálogo)
e alimento-composicao.csv (nutrientes por 100 g de cada alimento do catálogo, com a fonte de cada valor).
Regra do Guilherme (G-17): a TACO vale onde tem o nutriente; a planilha completa o resto e os alimentos que a TACO não tem."""
import sys, csv, re, unicodedata, collections
import openpyxl
import openpyxl.reader.excel as e
e.find_images = lambda *a, **k: ([], [])

CODIGOS = ["energia","gli","pro","lip","sat","mon","pol","trans","col","fib","na","k","ca","cu","i","mg","mn","p","se","zn","fe","fol","nia","pan","b2","b1","ret","vita","b6","b12","c","d","e","agua"]  # colunas 5..38 da aba
# chave da TACO -> (código, fator para a unidade da planilha). vita (mcg) fica só com a planilha: a TACO traz RE e RAE, não o mesmo número.
TACO = {"energia_kcal":("energia",1),"carboidrato_g":("gli",1),"proteina_g":("pro",1),"lipideos_g":("lip",1),"saturados_g":("sat",1),"monoinsaturados_g":("mon",1),
	"poliinsaturados_g":("pol",1),"colesterol_mg":("col",1),"fibra_g":("fib",1),"sodio_mg":("na",1),"potassio_mg":("k",1),"calcio_mg":("ca",1),"cobre_mg":("cu",1000),
	"magnesio_mg":("mg",1),"manganes_mg":("mn",1),"fosforo_mg":("p",1),"zinco_mg":("zn",1),"ferro_mg":("fe",1),"niacina_mg":("nia",1),"riboflavina_mg":("b2",1),
	"tiamina_mg":("b1",1),"retinol_ug":("ret",1),"piridoxina_mg":("b6",1),"vitamina_c_mg":("c",1),"umidade_g":("agua",1)}
ALIAS = {"Carne e ovo": "Carne ou ovo"}

def toks(s):
	s = unicodedata.normalize("NFD", s).encode("ascii", "ignore").decode().lower()
	return " ".join(re.sub(r"[^a-z0-9 ]", " ", s).split())
def fmt(v):
	v = round(float(v), 4); return ("%.4f" % v).rstrip("0").rstrip(".") if v != int(v) else str(int(v))

pasta = sys.argv[2].rstrip("/") + "/"
lido = lambda f: list(csv.DictReader(open(pasta + f, encoding="utf-8"), delimiter=";"))
catalogo = lido("alimentos.csv")
grupos = {g["id"]: g["nome"] for f in ("grupos-alimentos.csv", "grupos-alimentos-fase8.csv") for g in lido(f)}
taco = {toks(t["nome"]): t for t in reversed(lido("referencia-taco.csv"))}
rows = [r for r in openpyxl.load_workbook(sys.argv[1], read_only=True, data_only=True)["Alimentos"].iter_rows(values_only=True)]
cab = [i for i, r in enumerate(rows) if r[0] and "(100 g)" in str(r[0])]
mestre = collections.OrderedDict()  # grupo -> [linha]
blocos = collections.defaultdict(list)  # grupo -> [(titulo, {nome: linha})] porções, na ordem da planilha
for k, i in enumerate(cab):
	grupo = ALIAS.get(rows[i][0].replace(" (100 g)", "").strip(), rows[i][0].replace(" (100 g)", "").strip())
	j = i + 1; mestre[grupo] = []
	while j < len(rows) and rows[j][0]: mestre[grupo].append(rows[j]); j += 1
	fim = cab[k + 1] if k + 1 < len(cab) else len(rows)
	while j < fim:
		r = rows[j]
		if r[0] and r[1] == "Medida caseira":
			titulo = ALIAS.get(r[0].split(" - ")[0].strip(), r[0].split(" - ")[0].strip()); j += 1; b = {}
			while j < fim and rows[j][0] and rows[j][0] not in ("Média", "Desvio-padrão"): b.setdefault(toks(rows[j][0]), rows[j]); j += 1
			blocos[grupo].append((titulo, b))
		else: j += 1

por_grupo = collections.defaultdict(list)
for a in catalogo: por_grupo[grupos[a["grupo_id"]]].append(a)
id_grupo = {v: k for k, v in grupos.items()}
saida_alimentos, saida_comp, estat = [], [], collections.Counter()
novo = 0
for grupo, linhas in mestre.items():
	livres = list(por_grupo.get(grupo, []))
	for r in linhas:
		alvo = toks(r[0]); achado = next((a for a in livres if toks(a["nome"].split(" - ")[-1]) == alvo), None)
		if achado: livres.remove(achado); aid = achado["id"]; estat["catalogo"] += 1
		else:
			b = blocos[grupo][0][1].get(alvo) if blocos[grupo] else None
			if b is None: b = next((blk[alvo] for _, blk in blocos[grupo] if alvo in blk), None)
			novo += 1; aid = "019a0000-0001-7000-8000-%012d" % (500 + novo)
			if b is not None and b[2] and b[4] is not None:
				qtd = fmt(b[1]) if isinstance(b[1], (int, float)) else str(b[1]).strip()
				medida, gramas = f"{qtd.replace('.', ',')} {b[2].strip()}", fmt(b[4])
			else:
				# a planilha lista o alimento na composição mas em nenhuma porção (Tucumã e Umbu): gramas que dão as kcal da porção do grupo
				kcal100 = float(r[5]); kcal_porcao = float(next(g for g in lido("grupos-alimentos.csv") + lido("grupos-alimentos-fase8.csv") if g["nome"] == grupo)["kcal_por_porcao"])
				medida, gramas = "equivalente a 1 porção do grupo", fmt(round(kcal_porcao / kcal100 * 100)); estat["sem_porcao_na_planilha"] += 1
			saida_alimentos.append([aid, id_grupo[grupo], r[0].strip(), medida, gramas, "true"]); estat["novo"] += 1
		valores = {c: (r[5 + n], "PLANILHA") for n, c in enumerate(CODIGOS) if isinstance(r[5 + n], (int, float))}
		t = taco.get(alvo)
		if t:
			estat["taco"] += 1
			for chave, (cod, fator) in TACO.items():
				if t.get(chave) not in (None, ""): valores[cod] = (float(t[chave]) * fator, "TACO")
		for cod in CODIGOS:
			if cod in valores: saida_comp.append([aid, cod, fmt(valores[cod][0]), valores[cod][1]])
	if livres: raise SystemExit(f"alimentos do catálogo sem linha na planilha em {grupo}: {[a['nome'] for a in livres]}")
with open(pasta + "alimento-planilha.csv", "w", encoding="utf-8", newline="") as f:
	w = csv.writer(f, delimiter=";", lineterminator="\n"); w.writerow(["id", "grupo_id", "nome", "medida_caseira", "gramas_por_porcao", "ativo"]); w.writerows(saida_alimentos)
with open(pasta + "alimento-composicao.csv", "w", encoding="utf-8", newline="") as f:
	w = csv.writer(f, delimiter=";", lineterminator="\n"); w.writerow(["alimento_id", "nutriente", "valor", "fonte"]); w.writerows(saida_comp)
print(dict(estat), len(saida_comp), "valores")
