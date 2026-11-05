package com.br.dietamais.alimentos.infra;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.dominio.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Produtos da nutri (BE-48b). Toda consulta filtra por {@code nutricionista_id} vindo do {@code UsuarioAtual}, nunca do corpo. */
@Repository
public class ProdutosNutriStore {
	private final JdbcTemplate jdbc;private final JsonMapper mapper;
	public ProdutosNutriStore(JdbcTemplate jdbc,JsonMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
	public ProdutoResposta produto(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query("select * from produto_nutri where id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	public boolean criar(UUID id,UUID nutri,DadosProduto d,Instant agora){
		return jdbc.update("""
			insert into produto_nutri(id,nutricionista_id,nome,marca,classificacao,apresentacao,embalagem,sabores,medida_caseira,medida_g,observacao,nutrientes,ativo,criado_em,atualizado_em)
			values(?,?,?,?,?,?,?,?,?,?,?,?::jsonb,true,?,?) on conflict(id) do nothing
			""",id,nutri,d.nome(),d.marca(),d.classificacao(),d.apresentacao(),d.embalagem(),d.sabores(),d.medidaCaseira(),d.medidaGramas(),d.observacao(),json(d.nutrientes()),
			Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public void atualizar(UUID id,UUID nutri,DadosProduto d,Instant agora){
		jdbc.update("""
			update produto_nutri set nome=?,marca=?,classificacao=?,apresentacao=?,embalagem=?,sabores=?,medida_caseira=?,medida_g=?,observacao=?,nutrientes=?::jsonb,ativo=?,
			versao=versao+1,seq=nextval('seq_sync'),atualizado_em=?
			where id=? and nutricionista_id=? and excluido_em is null
			""",d.nome(),d.marca(),d.classificacao(),d.apresentacao(),d.embalagem(),d.sabores(),d.medidaCaseira(),d.medidaGramas(),d.observacao(),json(d.nutrientes()),d.ativo(),
			Timestamp.from(agora),id,nutri);
	}
	public PaginaProdutosResposta listar(UUID nutri,int pagina,int tamanho,String busca,String classificacao,boolean ativo){
		String where=" where nutricionista_id=? and ativo=? and excluido_em is null";var args=new ArrayList<Object>(List.of(nutri,ativo));
		if(classificacao!=null&&!classificacao.isBlank()){where+=" and classificacao=?";args.add(classificacao.strip());}
		if(busca!=null&&!busca.isBlank())for(var palavra:busca.strip().split("\\s+")){where+=" and (strpos(lower(nome),lower(?))>0 or strpos(lower(coalesce(marca,'')),lower(?))>0)";args.add(palavra);args.add(palavra);}
		long total=jdbc.queryForObject("select count(*) from produto_nutri"+where,Long.class,args.toArray());
		args.add(tamanho);args.add((long)pagina*tamanho);
		var itens=jdbc.query("select * from produto_nutri"+where+" order by lower(nome),id limit ? offset ?",this::mapear,args.toArray());
		return new PaginaProdutosResposta(itens,pagina,tamanho,total);
	}
	private String json(Map<String,BigDecimal> m){return mapper.writeValueAsString(m);}
	private ProdutoResposta mapear(ResultSet rs,int n) throws SQLException {
		Map<String,BigDecimal> nutrientes=mapper.readValue(rs.getString("nutrientes"),new TypeReference<TreeMap<String,BigDecimal>>(){});
		return new ProdutoResposta(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("marca"),rs.getString("classificacao"),rs.getString("apresentacao"),rs.getString("embalagem"),
			rs.getString("sabores"),rs.getString("medida_caseira"),rs.getBigDecimal("medida_g")==null?null:rs.getBigDecimal("medida_g").stripTrailingZeros(),rs.getString("observacao"),
			Collections.unmodifiableMap(nutrientes),rs.getBoolean("ativo"),rs.getLong("versao"),rs.getTimestamp("atualizado_em").toInstant());
	}
}
