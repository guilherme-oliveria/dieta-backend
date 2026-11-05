package com.br.dietamais.alimentos.infra;

import com.br.dietamais.alimentos.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class ProdutosParaDietaBanco implements ConsultarProdutosParaDieta {
	private final JdbcTemplate jdbc;private final JsonMapper mapper;
	public ProdutosParaDietaBanco(JdbcTemplate jdbc,JsonMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
	@Override public Map<UUID,ProdutoParaDieta> consultar(UUID nutri,Collection<UUID> ids){
		var r=new HashMap<UUID,ProdutoParaDieta>();if(ids.isEmpty())return r;
		jdbc.query("select id,nome,medida_caseira,ativo,nutrientes from produto_nutri where nutricionista_id=? and excluido_em is null and id=any(?)",rs->{
			Map<String,BigDecimal> n=mapper.readValue(rs.getString("nutrientes"),new TypeReference<TreeMap<String,BigDecimal>>(){});
			r.put(rs.getObject("id",UUID.class),new ProdutoParaDieta(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("medida_caseira"),rs.getBoolean("ativo"),ProdutoParaDieta.paraCodigos(n)));
		},nutri,ids.toArray(new UUID[0]));
		return r;
	}
}
