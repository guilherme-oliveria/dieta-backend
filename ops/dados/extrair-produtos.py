#!/usr/bin/env python3
"""Extrai a aba Produtos de "Dieta sem restrições.xlsm" para os CSVs de BE-48a.
Uso: python3 -I ops/dados/extrair-produtos.py <planilha.xlsm> <pasta-de-saida>
Gera referencia-produto.csv e referencia-produto-nutriente.csv (UTF-8, separador ';')."""
import sys, csv, openpyxl
import openpyxl.reader.excel as e
e.find_images = lambda *a, **k: ([], [])  # os gráficos da planilha quebram o openpyxl

# coluna da planilha -> chave do nutriente por 100 g (ou 100 mL). Chaves iguais às da TACO quando o nutriente é o mesmo.
NUTRIENTES = {8:'energia_kcal',9:'densidade_calorica_kcal_ml',10:'carboidrato_g',12:'polioes_g',13:'amido_g',14:'sacarose_g',15:'acucares_g',16:'lactose_g',
 17:'frutose_g',18:'glicose_g',19:'maltose_g',20:'outros_glicideos_g',21:'proteina_g',23:'proteina_soja_g',24:'outras_proteinas_g',25:'arginina_g',
 26:'taurina_mg',27:'l_carnitina_mg',28:'lipideos_g',30:'saturados_g',31:'monoinsaturados_g',32:'poliinsaturados_g',33:'trans_g',34:'alfa_linoleico_g',
 35:'alfa_linolenico_g',36:'araquidonico_mg',37:'docosahexaenoico_mg',39:'tcm_g',40:'colesterol_mg',41:'fibra_g',42:'fibra_soluvel_g',44:'fibra_insoluvel_g',
 46:'sodio_mg',47:'potassio_mg',48:'calcio_mg',49:'fosforo_mg',51:'ferro_mg',52:'cobre_ug',53:'iodo_ug',54:'magnesio_mg',55:'manganes_mg',56:'selenio_ug',
 57:'zinco_mg',58:'cloro_mg',59:'fluor_mg',60:'molibdenio_ug',61:'cromo_ug',62:'tiamina_mg',63:'riboflavina_mg',64:'niacina_mg',65:'acido_pantotenico_mg',
 66:'piridoxina_mg',67:'biotina_ug',68:'acido_folico_ug',69:'vitamina_b12_ug',70:'vitamina_a_ug',71:'vitamina_a_ui',72:'carotenoides_mg',73:'vitamina_c_mg',
 74:'vitamina_d_ug',75:'vitamina_d_ui',76:'vitamina_e_mg',77:'vitamina_e_ui',78:'vitamina_k_ug',79:'isoflavonas_mg',80:'inositol_mg',81:'colina_mg',
 90:'carga_solutos_renal_mosm_l'}
# colunas de texto: coluna -> campo
TEXTOS = [(0,'nome'),(1,'marca'),(2,'classificacao'),(3,'apresentacao'),(4,'embalagem'),(5,'sabores'),(6,'medida_caseira'),(82,'carboidratos_composicao'),
 (83,'proteinas_composicao'),(84,'lipideos_composicao'),(85,'fibras_composicao'),(86,'relacao_kcal_n'),(88,'osmolaridade'),(89,'osmolalidade'),
 (92,'diluicao_medida'),(93,'diluicao_gramas'),(94,'solvente_total'),(95,'solvente'),(96,'observacao')]

def txt(v):
    if v is None: return ''
    if isinstance(v, float) and v == int(v): v = int(v)
    return ' '.join(str(v).replace(';', ',').replace('"', "'").split())
def num(v):
    return '' if not isinstance(v,(int,float)) else repr(round(float(v),4)).rstrip('0').rstrip('.') if '.' in repr(round(float(v),4)) else repr(int(v))

wb = openpyxl.load_workbook(sys.argv[1], read_only=True, data_only=True)
linhas = [r for r in wb['Produtos'].iter_rows(values_only=True)][1:]
linhas = [r for r in linhas if r[0]]
saida = sys.argv[2]
campos = ['numero'] + [c for _, c in TEXTOS[:7]] + ['medida_g'] + [c for _, c in TEXTOS[7:]] + ['diluicao_medidas']
with open(f'{saida}/referencia-produto.csv','w',encoding='utf-8',newline='') as f, open(f'{saida}/referencia-produto-nutriente.csv','w',encoding='utf-8',newline='') as g:
    w = csv.writer(f, delimiter=';', lineterminator='\n'); wn = csv.writer(g, delimiter=';', lineterminator='\n')
    w.writerow(campos); wn.writerow(['produto','nutriente','valor'])
    for i, r in enumerate(linhas, 1):
        w.writerow([i] + [txt(r[c]) for c,_ in TEXTOS[:7]] + [num(r[7])] + [txt(r[c]) for c,_ in TEXTOS[7:]] + [num(r[91])])
        for col, chave in NUTRIENTES.items():
            if isinstance(r[col],(int,float)): wn.writerow([i, chave, num(r[col])])
print(len(linhas), 'produtos')
