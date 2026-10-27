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
	public boolean inserir(UUID id,UUID vinculo,UUID nutri,UUID paciente,String titulo,String orientacao,Instant agora){
		return jdbc.update("""
			insert into dieta(id,vinculo_id,nutricionista_id,paciente_id,titulo,orientacao,criado_em,atualizado_em) values(?,?,?,?,?,?,?,?) on conflict(id) do nothing
			""",id,vinculo,nutri,paciente,titulo,orientacao,Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public void atualizar(UUID id,UUID nutri,String titulo,String orientacao,Instant agora){
		jdbc.update("""
			update dieta set titulo=?,orientacao=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and excluido_em is null
			""",titulo,orientacao,Timestamp.from(agora),id,nutri);
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
			rs.getString("nome"),rs.getObject("horario",java.time.LocalTime.class),rs.getInt("ordem")),(Object)dietas.toArray(new UUID[0]));
	}
	public List<Item> itens(Collection<UUID> refeicoes){
		if(refeicoes.isEmpty())return List.of();
		return jdbc.query("select * from item_refeicao where refeicao_id=any(?) order by ordem,id",(rs,n)->new Item(rs.getObject("id",UUID.class),rs.getObject("refeicao_id",UUID.class),
			rs.getObject("alimento_id",UUID.class),rs.getBigDecimal("porcoes"),rs.getString("observacao"),rs.getInt("ordem")),(Object)refeicoes.toArray(new UUID[0]));
	}
	public void limparConteudo(UUID dieta){jdbc.update("delete from refeicao where dieta_id=?",dieta);}
	public boolean inserirRefeicao(Refeicao r){
		return jdbc.update("insert into refeicao(id,dieta_id,nome,horario,ordem) values(?,?,?,?,?) on conflict(id) do nothing",r.id(),r.dietaId(),r.nome(),Time.valueOf(r.horario()),r.ordem())==1;
	}
	public boolean inserirItem(Item i){
		return jdbc.update("insert into item_refeicao(id,refeicao_id,alimento_id,porcoes,observacao,ordem) values(?,?,?,?,?,?) on conflict(id) do nothing",i.id(),i.refeicaoId(),i.alimentoId(),i.porcoes(),i.observacao(),i.ordem())==1;
	}
	private Dieta mapear(ResultSet rs,int linha) throws SQLException {
		return new Dieta(rs.getObject("id",UUID.class),rs.getObject("vinculo_id",UUID.class),rs.getObject("paciente_id",UUID.class),rs.getString("titulo"),rs.getString("orientacao"),
			StatusDieta.valueOf(rs.getString("status")),rs.getLong("versao"),rs.getTimestamp("criado_em").toInstant(),rs.getTimestamp("atualizado_em").toInstant());
	}
}
