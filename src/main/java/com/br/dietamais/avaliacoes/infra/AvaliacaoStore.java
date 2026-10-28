package com.br.dietamais.avaliacoes.infra;

import com.br.dietamais.avaliacoes.api.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AvaliacaoStore {
	/** Colunas gravadas por inserção e por substituição, na ordem de {@link #valores}. */
	private static final String COLUNAS="data_hora,peso_kg,estatura_m,imc,classificacao_imc,"
		+"p_pescoco,p_torax,p_cintura,p_abdomen,p_quadril,p_braco,p_antebraco,p_coxa,p_panturrilha,"
		+"d_triceps,d_subescapular,d_peitoral,d_axilar_media,d_suprailiaca,d_abdominal,d_coxa,"
		+"percentual_gordura,peso_gordo_kg,peso_magro_kg,parecer,compartilhada";
	private static final int N=COLUNAS.split(",").length;
	private static final String SQL_INSERIR="insert into avaliacao(id,vinculo_id,nutricionista_id,paciente_id,"+COLUNAS+",criado_em,atualizado_em) values(?,?,?,?,"+"?,".repeat(N)+"?,?) on conflict(id) do nothing";
	private static final String SQL_ATUALIZAR="update avaliacao set "+String.join("=?,",COLUNAS.split(","))+"=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and excluido_em is null";
	private final JdbcTemplate jdbc;
	public AvaliacaoStore(JdbcTemplate jdbc){this.jdbc=jdbc;}

	public record Dados(Instant dataHora,BigDecimal pesoKg,BigDecimal estaturaM,BigDecimal imc,ClassificacaoImc classificacao,PerimetrosAvaliacao perimetros,
		DobrasAvaliacao dobras,ComposicaoAvaliacao composicao,String parecer,boolean compartilhada) {}
	public record Ficha(UUID id,UUID vinculoId,UUID pacienteId,long versao,Instant criadoEm) {}

	public void bloquear(UUID nutri){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"avaliacoes:"+nutri);}

	private static Object[] valores(Dados d){
		var p=d.perimetros();var b=d.dobras();var c=d.composicao();
		return new Object[]{Timestamp.from(d.dataHora()),d.pesoKg(),d.estaturaM(),d.imc(),d.classificacao().name(),
			p.pescoco(),p.torax(),p.cintura(),p.abdomen(),p.quadril(),p.braco(),p.antebraco(),p.coxa(),p.panturrilha(),
			b.triceps(),b.subescapular(),b.peitoral(),b.axilarMedia(),b.suprailiaca(),b.abdominal(),b.coxa(),
			c.percentualGordura(),c.pesoGordoKg(),c.pesoMagroKg(),d.parecer(),d.compartilhada()};
	}
	public boolean inserir(UUID id,UUID vinculo,UUID nutri,UUID paciente,Dados d,Instant agora){
		var args=new ArrayList<Object>(List.of(id,vinculo,nutri,paciente));args.addAll(Arrays.asList(valores(d)));args.add(Timestamp.from(agora));args.add(Timestamp.from(agora));
		return jdbc.update(SQL_INSERIR,args.toArray())==1;
	}
	public void atualizar(UUID id,UUID nutri,Dados d,Instant agora){
		var args=new ArrayList<Object>(Arrays.asList(valores(d)));args.add(Timestamp.from(agora));args.add(id);args.add(nutri);
		jdbc.update(SQL_ATUALIZAR,args.toArray());
	}
	public void registrarJustificativa(UUID avaliacao,UUID nutri,long versaoAnterior,String justificativa){
		jdbc.update("insert into avaliacao_justificativa(avaliacao_id,nutricionista_id,versao_anterior,justificativa) values(?,?,?,?)",avaliacao,nutri,versaoAnterior,justificativa);
	}
	public Ficha ficha(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query("select id,vinculo_id,paciente_id,versao,criado_em from avaliacao where id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),
			(rs,n)->new Ficha(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getLong(4),rs.getTimestamp(5).toInstant()),id,nutri).stream().findFirst().orElse(null);
	}
	public AvaliacaoResposta daNutri(UUID id,UUID nutri){
		return jdbc.query("select * from avaliacao where id=? and nutricionista_id=? and excluido_em is null",this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	public List<AvaliacaoResposta> porVinculo(UUID vinculo,UUID nutri){
		return jdbc.query("select * from avaliacao where vinculo_id=? and nutricionista_id=? and excluido_em is null order by data_hora desc,id desc",this::mapear,vinculo,nutri);
	}
	/** Só as compartilhadas (RN-AVA-05) de vínculos ativos ou encerrados (histórico autorizado). */
	public List<AvaliacaoResposta> compartilhadasDoPaciente(UUID paciente){
		return jdbc.query("""
			select a.* from avaliacao a join vinculo v on v.id=a.vinculo_id
			where a.paciente_id=? and a.compartilhada and a.excluido_em is null and v.status in ('ATIVO','ENCERRADO') and v.excluido_em is null order by a.data_hora desc,a.id desc
			""",this::mapear,paciente);
	}
	public AvaliacaoResposta compartilhadaDoPaciente(UUID id,UUID paciente){
		return jdbc.query("""
			select a.* from avaliacao a join vinculo v on v.id=a.vinculo_id
			where a.id=? and a.paciente_id=? and a.compartilhada and a.excluido_em is null and v.status in ('ATIVO','ENCERRADO') and v.excluido_em is null
			""",this::mapear,id,paciente).stream().findFirst().orElse(null);
	}
	private AvaliacaoResposta mapear(ResultSet rs,int linha) throws SQLException {
		return new AvaliacaoResposta(rs.getObject("id",UUID.class),rs.getObject("paciente_id",UUID.class),rs.getObject("vinculo_id",UUID.class),rs.getTimestamp("data_hora").toInstant(),
			rs.getBigDecimal("peso_kg"),rs.getBigDecimal("estatura_m"),rs.getBigDecimal("imc"),ClassificacaoImc.valueOf(rs.getString("classificacao_imc")),
			new PerimetrosAvaliacao(rs.getBigDecimal("p_pescoco"),rs.getBigDecimal("p_torax"),rs.getBigDecimal("p_cintura"),rs.getBigDecimal("p_abdomen"),rs.getBigDecimal("p_quadril"),
				rs.getBigDecimal("p_braco"),rs.getBigDecimal("p_antebraco"),rs.getBigDecimal("p_coxa"),rs.getBigDecimal("p_panturrilha")),
			new DobrasAvaliacao(rs.getBigDecimal("d_triceps"),rs.getBigDecimal("d_subescapular"),rs.getBigDecimal("d_peitoral"),rs.getBigDecimal("d_axilar_media"),
				rs.getBigDecimal("d_suprailiaca"),rs.getBigDecimal("d_abdominal"),rs.getBigDecimal("d_coxa")),
			new ComposicaoAvaliacao(rs.getBigDecimal("percentual_gordura"),rs.getBigDecimal("peso_gordo_kg"),rs.getBigDecimal("peso_magro_kg")),
			rs.getString("parecer"),rs.getBoolean("compartilhada"),rs.getLong("versao"),rs.getTimestamp("criado_em").toInstant(),rs.getTimestamp("atualizado_em").toInstant());
	}
}
