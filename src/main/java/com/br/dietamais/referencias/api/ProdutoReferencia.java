package com.br.dietamais.referencias.api;

import java.math.BigDecimal;
import java.util.*;

/**
 * Produto ou suplemento do catálogo base (BE-48a), copiado da aba Produtos da planilha da nutri. {@code nutrientes} são por 100 g (ou 100 mL, para líquidos)
 * e só trazem o que a planilha informa (RN-CAL-04: sem dado não vira zero). As chaves seguem as da TACO quando o nutriente é o mesmo
 * (energia_kcal, proteina_g, lipideos_g, carboidrato_g, fibra_g, sodio_mg...) e acrescentam os que só a planilha tem (iodo_ug, taurina_mg, vitamina_k_ug...).
 * {@code medidaGramas} é o peso (g ou mL) de uma {@code medidaCaseira}. Campos de texto vazios na planilha vêm {@code null}.
 */
public record ProdutoReferencia(int numero,String nome,String marca,String classificacao,String apresentacao,String embalagem,String sabores,
		String medidaCaseira,BigDecimal medidaGramas,Composicao composicao,String relacaoKcalN,String osmolaridade,String osmolalidade,Diluicao diluicao,
		String observacao,Map<String,BigDecimal> nutrientes){
	public static final String FONTE="Planilha \"Dieta sem restrições\" (aba Produtos), da nutricionista Luciana; valores dos rótulos e das tabelas dos fabricantes, como transcritos nela";
	/** Origem de cada macronutriente, em texto livre como na planilha (ex.: "amido de milho hidrolisado (85%), sacarose (10%)"). */
	public record Composicao(String carboidratos,String proteinas,String lipideos,String fibras){}
	/** Como preparar: {@code medidas} medidas caseiras ({@code medida}) = {@code gramas} g, em {@code solventeTotal} mL de {@code solvente}. Textos porque a planilha traz faixas ("15 a 30", "qualquer quantidade"). */
	public record Diluicao(BigDecimal medidas,String medida,String gramas,String solventeTotal,String solvente){}
}
