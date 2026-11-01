package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code grupo_porcao} e {@code grupo_porcao_nutriente} uma vez (só mudam por changeset, 31 porções) e guarda em memória. */
@Component
class ConsultaPorcoesBanco implements ConsultaPorcoes {
	private final JdbcTemplate jdbc;
	private volatile PorcoesGrupos dados;
	ConsultaPorcoesBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	@Override public PorcoesGrupos porcoes(){
		var d=dados;
		if(d==null){
			synchronized(this){
				if(dados==null)dados=carregar();
				d=dados;
			}
		}
		return d;
	}
	private PorcoesGrupos carregar(){
		var nutrientes=new HashMap<UUID,Map<String,BigDecimal>>();
		jdbc.query("select porcao_id,nutriente,valor from grupo_porcao_nutriente",rs->{
			nutrientes.computeIfAbsent(rs.getObject("porcao_id",UUID.class),k->new LinkedHashMap<>()).put(rs.getString("nutriente"),rs.getBigDecimal("valor"));
		});
		var grupos=new LinkedHashMap<UUID,List<PorcoesGrupos.Opcao>>();var nomes=new HashMap<UUID,String>();
		jdbc.query("select p.id,p.grupo_id,g.nome,p.opcao,p.kcal,p.quantidade,p.unidade,p.medida_caseira,p.guia_alimentar from grupo_porcao p join grupo_alimento g on g.id=p.grupo_id order by g.id,p.opcao",rs->{
			var gid=rs.getObject("grupo_id",UUID.class);nomes.put(gid,rs.getString("nome"));
			var ordenados=new LinkedHashMap<String,BigDecimal>();
			var n=nutrientes.getOrDefault(rs.getObject("id",UUID.class),Map.of());
			for(var cod:PorcoesGrupos.UNIDADES.keySet())if(n.containsKey(cod))ordenados.put(cod,n.get(cod));
			grupos.computeIfAbsent(gid,k->new ArrayList<>()).add(new PorcoesGrupos.Opcao(rs.getInt("opcao"),rs.getBigDecimal("kcal"),rs.getBigDecimal("quantidade"),rs.getString("unidade"),rs.getString("medida_caseira"),rs.getBoolean("guia_alimentar"),Collections.unmodifiableMap(ordenados)));
		});
		var lista=new ArrayList<PorcoesGrupos.Grupo>();
		grupos.forEach((id,o)->lista.add(new PorcoesGrupos.Grupo(id,nomes.get(id),List.copyOf(o))));
		return new PorcoesGrupos(PorcoesGrupos.FONTE,PorcoesGrupos.UNIDADES,List.copyOf(lista));
	}
}
