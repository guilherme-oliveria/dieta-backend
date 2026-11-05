package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.referencias.api.PorcoesGrupos;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Confere os CSVs da BE-47c sem banco: composição de todos os alimentos do catálogo, alimentos novos coerentes e a fonte de cada valor. */
class ComposicaoAlimentosCsvTest {
	List<Map<String,String>> ler(String arquivo) throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/"+arquivo)){
			var linhas=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");
			var cab=linhas[0].split(";",-1);var r=new ArrayList<Map<String,String>>();
			for(int i=1;i<linhas.length;i++){var c=linhas[i].split(";",-1);assertThat(c).as("linha %d de %s",i+1,arquivo).hasSameSizeAs(cab);var m=new HashMap<String,String>();for(int j=0;j<cab.length;j++)m.put(cab[j],c[j]);r.add(m);}
			return r;
		}
	}

	@Test void todoAlimentoDoCatalogoTemComposicaoComMacrosEFonteValida() throws Exception {
		var ids=new HashSet<String>();ler("alimentos.csv").forEach(m->ids.add(m.get("id")));ler("alimento-planilha.csv").forEach(m->ids.add(m.get("id")));
		assertThat(ids).hasSize(482+89);
		var por=new HashMap<String,Map<String,String>>();
		for(var m:ler("alimento-composicao.csv")){
			assertThat(PorcoesGrupos.UNIDADES).containsKey(m.get("nutriente"));assertThat(m.get("fonte")).isIn("TACO","PLANILHA");assertThat(Double.parseDouble(m.get("valor"))).isGreaterThanOrEqualTo(0);
			assertThat(por.computeIfAbsent(m.get("alimento_id"),k->new HashMap<>()).put(m.get("nutriente"),m.get("fonte"))).as("repetido %s/%s",m.get("alimento_id"),m.get("nutriente")).isNull();
		}
		assertThat(por.keySet()).isEqualTo(ids);
		por.values().forEach(n->assertThat(n).containsKeys("energia","gli","pro","lip"));
		assertThat(por.values().stream().filter(n->n.containsValue("TACO")).count()).isBetween(40L,200L);
	}
	@Test void alimentosNovosTemGrupoMedidaEGramasEIdsAcimaDeQuinhentos() throws Exception {
		var novos=ler("alimento-planilha.csv");
		assertThat(novos).hasSize(89);
		var grupos=new HashSet<String>();
		for(var m:novos){
			assertThat(m.get("id")).isGreaterThanOrEqualTo("019a0000-0001-7000-8000-000000000501");assertThat(m.get("nome")).isNotBlank();assertThat(m.get("medida_caseira")).isNotBlank();
			assertThat(Double.parseDouble(m.get("gramas_por_porcao"))).isPositive();grupos.add(m.get("grupo_id"));
		}
		// Embutido (15), Vegetal cru ou cozido (53), Bebida (19) e 2 frutas
		assertThat(grupos).hasSize(4);
		assertThat(novos.stream().filter(m->m.get("grupo_id").endsWith("14")).count()).isEqualTo(15);
		assertThat(novos.stream().filter(m->m.get("grupo_id").endsWith("13")).count()).isEqualTo(53);
		assertThat(novos.stream().filter(m->m.get("grupo_id").endsWith("15")).count()).isEqualTo(19);
	}
	@Test void azeiteDeOlivaBateComAPlanilha() throws Exception {
		var catalogo=ler("alimentos.csv").stream().filter(m->m.get("nome").endsWith("Azeite de oliva")).findFirst().orElseThrow();
		var comp=new HashMap<String,String>();for(var m:ler("alimento-composicao.csv"))if(m.get("alimento_id").equals(catalogo.get("id")))comp.put(m.get("nutriente"),m.get("valor"));
		assertThat(comp).containsEntry("energia","884").containsEntry("lip","100").containsEntry("ca","0.18");
	}
}
