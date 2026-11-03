package com.br.dietamais.dietas.infra;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Modelos de dieta (BE-52). {@code nutricionistaId} nulo é modelo do sistema. */
@Repository
public class ModeloDietaStore {
	private final JdbcTemplate jdbc;
	public ModeloDietaStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public record Linha(UUID id,UUID nutricionistaId,String titulo,String descricao,String condicao,String orientacao,String rodape,String porcoesJson,String metasJson,
		List<String> restricoes,BigDecimal kcalReferencia,String refeicoesJson,long versao,Instant criadoEm,Instant atualizadoEm) {}
	/** Campos editáveis, com os jsons já serializados. */
	public record Conteudo(String titulo,String descricao,String condicao,String orientacao,String rodape,String porcoesJson,String metasJson,List<String> restricoes,BigDecimal kcalReferencia,String refeicoesJson) {}
	private static final String BASE="select id,nutricionista_id,titulo,descricao,condicao,orientacao,rodape,porcoes::text porcoes,metas::text metas,restricoes,kcal_referencia,refeicoes::text refeicoes,versao,criado_em,atualizado_em from modelo_dieta where excluido_em is null";
	public void bloquear(UUID nutri){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"modelos-dieta:"+nutri);}
	/** Do sistema primeiro, depois os da nutri. */
	public List<Linha> visiveis(UUID nutri){
		return jdbc.query(BASE+" and (nutricionista_id is null or nutricionista_id=?) order by (nutricionista_id is null) desc,titulo,id",this::mapear,nutri);
	}
	public Linha visivel(UUID id,UUID nutri){
		return jdbc.query(BASE+" and id=? and (nutricionista_id is null or nutricionista_id=?)",this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	public Linha daNutri(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query(BASE+" and id=? and nutricionista_id=?"+(bloquear?" for update":""),this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	public int contar(UUID nutri){return jdbc.queryForObject("select count(*) from modelo_dieta where nutricionista_id=? and excluido_em is null",Integer.class,nutri);}
	public boolean inserir(UUID id,UUID nutri,Conteudo c,Instant agora){
		return jdbc.update("""
			insert into modelo_dieta(id,nutricionista_id,titulo,descricao,condicao,orientacao,rodape,porcoes,metas,restricoes,kcal_referencia,refeicoes,criado_em,atualizado_em)
			values(?,?,?,?,?,?,?,?::jsonb,?::jsonb,?::text[],?,?::jsonb,?,?) on conflict(id) do nothing
			""",id,nutri,c.titulo(),c.descricao(),c.condicao(),c.orientacao(),c.rodape(),c.porcoesJson(),c.metasJson(),texto(c.restricoes()),c.kcalReferencia(),c.refeicoesJson(),Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public boolean atualizar(UUID id,UUID nutri,long versao,Conteudo c,Instant agora){
		return jdbc.update("""
			update modelo_dieta set titulo=?,descricao=?,condicao=?,orientacao=?,rodape=?,porcoes=?::jsonb,metas=?::jsonb,restricoes=?::text[],kcal_referencia=?,refeicoes=?::jsonb,
			versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and versao=? and excluido_em is null
			""",c.titulo(),c.descricao(),c.condicao(),c.orientacao(),c.rodape(),c.porcoesJson(),c.metasJson(),texto(c.restricoes()),c.kcalReferencia(),c.refeicoesJson(),Timestamp.from(agora),id,nutri,versao)==1;
	}
	public boolean excluir(UUID id,UUID nutri,long versao,Instant agora){
		return jdbc.update("update modelo_dieta set excluido_em=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and versao=? and excluido_em is null",
			Timestamp.from(agora),Timestamp.from(agora),id,nutri,versao)==1;
	}
	private static String texto(List<String> lista){return "{"+String.join(",",lista)+"}";}
	private Linha mapear(ResultSet rs,int n) throws SQLException {
		var arr=rs.getArray("restricoes");var restricoes=arr==null?List.<String>of():List.of((String[])arr.getArray());
		return new Linha(rs.getObject("id",UUID.class),rs.getObject("nutricionista_id",UUID.class),rs.getString("titulo"),rs.getString("descricao"),rs.getString("condicao"),rs.getString("orientacao"),rs.getString("rodape"),
			rs.getString("porcoes"),rs.getString("metas"),restricoes,rs.getBigDecimal("kcal_referencia"),rs.getString("refeicoes"),rs.getLong("versao"),rs.getTimestamp("criado_em").toInstant(),rs.getTimestamp("atualizado_em").toInstant());
	}
}
