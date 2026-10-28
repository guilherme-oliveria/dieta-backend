package com.br.dietamais.agenda.infra;

import com.br.dietamais.agenda.api.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaStore {
	private static final String SQL_SELECT="select id,paciente_id,vinculo_id,inicio,duracao_min,tipo,lugar,status,versao,criado_em,atualizado_em from consulta ";
	private final JdbcTemplate jdbc;
	public ConsultaStore(JdbcTemplate jdbc){this.jdbc=jdbc;}

	public record Dados(Instant inicio,int duracaoMin,TipoConsulta tipo,String lugar,StatusConsulta status) {}

	public void bloquear(UUID nutri){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"consultas:"+nutri);}

	public boolean inserir(UUID id,UUID vinculo,UUID nutri,UUID paciente,Dados d,Instant agora){
		return jdbc.update("insert into consulta(id,vinculo_id,nutricionista_id,paciente_id,inicio,duracao_min,tipo,lugar,status,criado_em,atualizado_em) values(?,?,?,?,?,?,?,?,?,?,?) on conflict(id) do nothing",
			id,vinculo,nutri,paciente,Timestamp.from(d.inicio()),d.duracaoMin(),d.tipo().name(),d.lugar(),d.status().name(),Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public void atualizar(UUID id,UUID nutri,Dados d,Instant agora){
		jdbc.update("update consulta set inicio=?,duracao_min=?,tipo=?,lugar=?,status=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and excluido_em is null",
			Timestamp.from(d.inicio()),d.duracaoMin(),d.tipo().name(),d.lugar(),d.status().name(),Timestamp.from(agora),id,nutri);
	}
	public ConsultaResposta daNutri(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query(SQL_SELECT+"where id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	/** Agenda da nutri na janela [de, ate), opcionalmente de um paciente; mais cedo primeiro. */
	public List<ConsultaResposta> daNutri(UUID nutri,UUID paciente,Instant de,Instant ate){
		var sql=SQL_SELECT+"where nutricionista_id=? and excluido_em is null and inicio>=? and inicio<?"+(paciente==null?"":" and paciente_id=?")+" order by inicio,id limit 500";
		var args=new ArrayList<Object>(List.of(nutri,Timestamp.from(de),Timestamp.from(ate)));if(paciente!=null)args.add(paciente);
		return jdbc.query(sql,this::mapear,args.toArray());
	}
	/** Consultas do paciente em vínculos ativos ou encerrados (histórico autorizado). */
	public List<ConsultaResposta> doPaciente(UUID paciente,Instant de,Instant ate){
		return jdbc.query("select c.id,c.paciente_id,c.vinculo_id,c.inicio,c.duracao_min,c.tipo,c.lugar,c.status,c.versao,c.criado_em,c.atualizado_em from consulta c join vinculo v on v.id=c.vinculo_id "
			+"where c.paciente_id=? and c.excluido_em is null and v.status in ('ATIVO','ENCERRADO') and v.excluido_em is null and c.inicio>=? and c.inicio<? order by c.inicio,c.id limit 500",
			this::mapear,paciente,Timestamp.from(de),Timestamp.from(ate));
	}
	public ConsultaResposta doPaciente(UUID id,UUID paciente){
		return jdbc.query("select c.id,c.paciente_id,c.vinculo_id,c.inicio,c.duracao_min,c.tipo,c.lugar,c.status,c.versao,c.criado_em,c.atualizado_em from consulta c join vinculo v on v.id=c.vinculo_id "
			+"where c.id=? and c.paciente_id=? and c.excluido_em is null and v.status in ('ATIVO','ENCERRADO') and v.excluido_em is null",this::mapear,id,paciente).stream().findFirst().orElse(null);
	}
	private ConsultaResposta mapear(ResultSet rs,int linha) throws SQLException {
		return new ConsultaResposta(rs.getObject("id",UUID.class),rs.getObject("paciente_id",UUID.class),rs.getObject("vinculo_id",UUID.class),rs.getTimestamp("inicio").toInstant(),rs.getInt("duracao_min"),
			TipoConsulta.valueOf(rs.getString("tipo")),rs.getString("lugar"),StatusConsulta.valueOf(rs.getString("status")),rs.getLong("versao"),rs.getTimestamp("criado_em").toInstant(),rs.getTimestamp("atualizado_em").toInstant());
	}
}
