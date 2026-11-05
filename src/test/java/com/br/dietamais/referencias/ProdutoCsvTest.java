package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Confere os CSVs do catálogo de produtos (BE-48a) sem banco: 400 produtos da planilha, nutrientes só dos produtos existentes e valores como na fonte. */
class ProdutoCsvTest {
	List<Map<String,String>> ler(String arquivo) throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/"+arquivo)){
			var linhas=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");
			var cab=linhas[0].split(";",-1);var r=new ArrayList<Map<String,String>>();
			for(int i=1;i<linhas.length;i++){var c=linhas[i].split(";",-1);assertThat(c).as("linha %d de %s",i+1,arquivo).hasSameSizeAs(cab);var m=new HashMap<String,String>();for(int j=0;j<cab.length;j++)m.put(cab[j],c[j]);r.add(m);}
			return r;
		}
	}

	@Test void tem400ProdutosComNumeroSequencialENomeEClassificacao() throws Exception {
		var r=ler("referencia-produto.csv");
		assertThat(r).hasSize(400);
		for(int i=0;i<r.size();i++)assertThat(r.get(i).get("numero")).isEqualTo(String.valueOf(i+1));
		assertThat(r).allSatisfy(m->{assertThat(m.get("nome")).isNotBlank();assertThat(m.get("classificacao")).isNotBlank();assertThat(m.get("apresentacao")).isNotBlank();});
		assertThat(r.stream().map(m->m.get("classificacao")).distinct()).hasSize(13);
	}
	@Test void todoProdutoTemOsQuatroMacrosEKcalENenhumNutrienteRepetidoOuOrfao() throws Exception {
		var nut=ler("referencia-produto-nutriente.csv");
		var por=new HashMap<String,Map<String,String>>();
		for(var m:nut){assertThat(por.computeIfAbsent(m.get("produto"),k->new HashMap<>()).put(m.get("nutriente"),m.get("valor"))).as("repetido %s/%s",m.get("produto"),m.get("nutriente")).isNull();assertThat(Double.parseDouble(m.get("valor"))).isGreaterThanOrEqualTo(0);}
		assertThat(por.keySet()).hasSize(400);
		for(int i=1;i<=400;i++)assertThat(por.get(String.valueOf(i))).containsKeys("energia_kcal","carboidrato_g","proteina_g","lipideos_g");
	}
	@Test void primeiroProdutoBateComAPlanilha() throws Exception {
		var p=ler("referencia-produto.csv").get(0);
		assertThat(p.get("nome")).isEqualTo("100% Pure Whey Protein");assertThat(p.get("marca")).isEqualTo("Probiótica");
		assertThat(p.get("apresentacao")).isEqualTo("pó");assertThat(p.get("medida_g")).isEqualTo("15.5");assertThat(p.get("solvente")).isEqualTo("água");
		var n=new HashMap<String,String>();for(var m:ler("referencia-produto-nutriente.csv"))if(m.get("produto").equals("1"))n.put(m.get("nutriente"),m.get("valor"));
		assertThat(n).containsEntry("energia_kcal","374.19").containsEntry("proteina_g","74.19").containsEntry("carboidrato_g","6.45").containsEntry("lipideos_g","5.97").containsEntry("sodio_mg","235.48").doesNotContainKey("colesterol_mg");
	}
}
