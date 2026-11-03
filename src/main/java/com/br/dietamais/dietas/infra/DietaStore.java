package com.br.dietamais.dietas.infra;

import com.br.dietamais.dietas.api.StatusDieta;
import com.br.dietamais.dietas.dominio.LinhasDieta.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DietaStore {
	private final JdbcTemplate jdbc;
	public DietaStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public void bloquear(UUID nutri){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"dietas:"+nutri);}
	/** Campos da dieta por porções (BE-49a): jsons já serializados, nulos quando não informados. */
	/** {@code restricoes} nulo = não informadas: ao inserir fica vazio, ao atualizar mantém as atuais (BE-52b). */
	public record Extras(String porcoesJson,String metasJson,java.math.BigDecimal pesoReferenciaKg,String rodape,java.util.List<String> restricoes) {
		public Extras(String porcoesJson,String metasJson,java.math.BigDecimal pesoReferenciaKg,String rodape){this(porcoesJson,metasJson,pesoReferenciaKg,rodape,null);}
		public static final Extras VAZIO=new Extras(null,null,null,null);
	}
	public boolean inserir(UUID id,UUID vinculo,UUID nutri,UUID paciente,String titulo,String orientacao,Extras x,Instant agora){
		return jdbc.update("""
			insert into dieta(id,vinculo_id,nutricionista_id,paciente_id,titulo,orientacao,porcoes,metas,peso_referencia_kg,rodape,restricoes,criado_em,atualizado_em) values(?,?,?,?,?,?,?::jsonb,?::jsonb,?,?,?::text[],?,?) on conflict(id) do nothing
			""",id,vinculo,nutri,paciente,titulo,orientacao,x.porcoesJson(),x.metasJson(),x.pesoReferenciaKg(),x.rodape(),texto(x.restricoes()==null?java.util.List.of():x.restricoes()),Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public void atualizar(UUID id,UUID nutri,String titulo,String orientacao,Extras x,Instant agora){
		jdbc.update("""
			update dieta set titulo=?,orientacao=?,porcoes=?::jsonb,metas=?::jsonb,peso_referencia_kg=?,rodape=?,restricoes=coalesce(?::text[],restricoes),versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and excluido_em is null
			""",titulo,orientacao,x.porcoesJson(),x.metasJson(),x.pesoReferenciaKg(),x.rodape(),x.restricoes()==null?null:texto(x.restricoes()),Timestamp.from(agora),id,nutri);
	}
	public Dieta dieta(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query("select * from dieta where id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	public List<Dieta> porVinculo(UUID vinculo,UUID nutri){
		return jdbc.query("select * from dieta where vinculo_id=? and nutricionista_id=? and excluido_em is null order by criado_em desc,id desc",this::mapear,vinculo,nutri);
	}
	public List<Refeicao> refeicoes(Collection<UUID> dietas){
		if(dietas.isEmpty())return List.of();
		return jdbc.query("select * from refeicao where dieta_id=any(?) order by ordem,horario,id",(rs,n)->new Refeicao(rs.getObject("id",UUID.class),rs.getObject("dieta_id",UUID.class),
			rs.getString("nome"),rs.getObject("horario",java.time.LocalTime.class),rs.getInt("ordem"),rs.getString("tipo"),rs.getObject("alternativa_de",UUID.class)),(Object)dietas.toArray(new UUID[0]));
	}
	public List<Item> itens(Collection<UUID> refeicoes){
		if(refeicoes.isEmpty())return List.of();
		return jdbc.query("select * from item_refeicao where refeicao_id=any(?) order by ordem,id",(rs,n)->new Item(rs.getObject("id",UUID.class),rs.getObject("refeicao_id",UUID.class),
			rs.getObject("alimento_id",UUID.class),rs.getObject("grupo_id",UUID.class),rs.getBigDecimal("porcoes"),rs.getBoolean("a_vontade"),rs.getString("texto_livre"),rs.getString("observacao"),rs.getInt("ordem")),(Object)refeicoes.toArray(new UUID[0]));
	}
	public void substituirPublicada(UUID vinculo,UUID exceto,Instant agora){
		jdbc.update("update dieta set status='SUBSTITUIDA',versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where vinculo_id=? and id<>? and status='PUBLICADA' and excluido_em is null",Timestamp.from(agora),vinculo,exceto);
	}
	public void marcarPublicada(UUID id,UUID nutri,Instant agora){
		jdbc.update("update dieta set status='PUBLICADA',versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and excluido_em is null",Timestamp.from(agora),id,nutri);
	}
	public UUID inserirSnapshot(UUID id,UUID dieta,UUID vinculo,UUID nutri,UUID paciente,String conteudo,Instant publicadoEm){
		jdbc.update("insert into dieta_snapshot(id,dieta_id,vinculo_id,nutricionista_id,paciente_id,conteudo,publicado_em) values(?,?,?,?,?,?::jsonb,?)",id,dieta,vinculo,nutri,paciente,conteudo,Timestamp.from(publicadoEm));
		return id;
	}
	public record SnapshotRecebido(UUID id,UUID dietaId,UUID vinculoId,UUID nutricionistaId,String conteudo,String statusDieta,String statusVinculo) {}
	/** Snapshots do paciente em vínculos ativos ou encerrados (histórico autorizado), mais recentes primeiro. */
	public List<SnapshotRecebido> snapshotsDoPaciente(UUID paciente){
		return jdbc.query("""
			select s.id,s.dieta_id,s.vinculo_id,s.nutricionista_id,s.conteudo::text conteudo,d.status status_dieta,v.status status_vinculo
			from dieta_snapshot s join dieta d on d.id=s.dieta_id join vinculo v on v.id=s.vinculo_id
			where s.paciente_id=? and v.status in ('ATIVO','ENCERRADO') and v.excluido_em is null order by s.publicado_em desc,s.id desc
			""",(rs,n)->new SnapshotRecebido(rs.getObject("id",UUID.class),rs.getObject("dieta_id",UUID.class),rs.getObject("vinculo_id",UUID.class),rs.getObject("nutricionista_id",UUID.class),
				rs.getString("conteudo"),rs.getString("status_dieta"),rs.getString("status_vinculo")),paciente);
	}
	/** Conteúdo JSON do snapshot da dieta, só se for da nutri. */
	public String snapshotDaNutri(UUID dieta,UUID nutri){
		return jdbc.query("select conteudo::text from dieta_snapshot where dieta_id=? and nutricionista_id=?",(rs,n)->rs.getString(1),dieta,nutri).stream().findFirst().orElse(null);
	}
	public record Selecao(UUID dietaId,long versao) {}
	public Selecao selecao(UUID paciente,boolean bloquear){
		return jdbc.query("select dieta_id,versao from dieta_em_uso where paciente_id=?"+(bloquear?" for update":""),(rs,n)->new Selecao(rs.getObject(1,UUID.class),rs.getLong(2)),paciente).stream().findFirst().orElse(null);
	}
	public void bloquearPaciente(UUID paciente){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"dieta-em-uso:"+paciente);}
	public void gravarSelecao(UUID paciente,UUID dieta,Instant agora){
		jdbc.update("""
			insert into dieta_em_uso(paciente_id,dieta_id,atualizado_em) values(?,?,?)
			on conflict(paciente_id) do update set dieta_id=excluded.dieta_id,versao=dieta_em_uso.versao+1,seq=nextval('seq_sync'),atualizado_em=excluded.atualizado_em
			""",paciente,dieta,Timestamp.from(agora));
	}
	public void limparConteudo(UUID dieta){jdbc.update("delete from refeicao where dieta_id=?",dieta);}
	public boolean inserirRefeicao(Refeicao r){
		return jdbc.update("insert into refeicao(id,dieta_id,nome,horario,ordem,tipo,alternativa_de) values(?,?,?,?,?,?,?) on conflict(id) do nothing",r.id(),r.dietaId(),r.nome(),Time.valueOf(r.horario()),r.ordem(),r.tipo(),r.alternativaDe())==1;
	}
	public boolean inserirItem(Item i){
		return jdbc.update("insert into item_refeicao(id,refeicao_id,alimento_id,grupo_id,porcoes,a_vontade,texto_livre,observacao,ordem) values(?,?,?,?,?,?,?,?,?) on conflict(id) do nothing",i.id(),i.refeicaoId(),i.alimentoId(),i.grupoId(),i.porcoes(),i.aVontade(),i.textoLivre(),i.observacao(),i.ordem())==1;
	}
	private Dieta mapear(ResultSet rs,int linha) throws SQLException {
		return new Dieta(rs.getObject("id",UUID.class),rs.getObject("vinculo_id",UUID.class),rs.getObject("paciente_id",UUID.class),rs.getString("titulo"),rs.getString("orientacao"),
			StatusDieta.valueOf(rs.getString("status")),rs.getLong("versao"),rs.getTimestamp("criado_em").toInstant(),rs.getTimestamp("atualizado_em").toInstant(),
			rs.getString("porcoes"),rs.getString("metas"),rs.getBigDecimal("peso_referencia_kg"),rs.getString("rodape"),restricoes(rs));
	}
	private static String texto(java.util.List<String> lista){return "{"+String.join(",",lista)+"}";}
	private static java.util.List<String> restricoes(ResultSet rs) throws SQLException {
		var a=rs.getArray("restricoes");return a==null?java.util.List.of():java.util.List.of((String[])a.getArray());
	}
}
