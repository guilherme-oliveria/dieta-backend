package com.br.dietamais.relatorios.infra;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExportacaoStore {
	public record Pedido(UUID id,UUID usuarioId,String status,String chave,int tentativas,Instant criadoEm,Instant prontaEm,Instant expiraEm) { }
	private static final String COLUNAS="id,usuario_id,status,chave,tentativas,criado_em,pronta_em,expira_em";
	private final JdbcTemplate jdbc;
	public ExportacaoStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private static Pedido mapear(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
		return new Pedido(rs.getObject("id",UUID.class),rs.getObject("usuario_id",UUID.class),rs.getString("status"),rs.getString("chave"),rs.getInt("tentativas"),
			rs.getTimestamp("criado_em").toInstant(),instante(rs.getTimestamp("pronta_em")),instante(rs.getTimestamp("expira_em")));
	}
	private static Instant instante(Timestamp t){return t==null?null:t.toInstant();}
	public void inserir(UUID id,UUID usuario,Instant agora){
		jdbc.update("insert into exportacao_dados(id,usuario_id,status,criado_em) values(?,?,'PENDENTE',?)",id,usuario,Timestamp.from(agora));
	}
	/** Serializa os pedidos do mesmo titular. */
	public void bloquearUsuario(UUID usuario){jdbc.query("select id from usuario where id=? for update",(rs,n)->0,usuario);}
	public Optional<Pedido> maisRecente(UUID usuario){
		return jdbc.query("select "+COLUNAS+" from exportacao_dados where usuario_id=? order by criado_em desc,id desc limit 1",ExportacaoStore::mapear,usuario).stream().findFirst();
	}
	public Optional<Pedido> proximoPendente(){
		return jdbc.query("select "+COLUNAS+" from exportacao_dados where status='PENDENTE' order by criado_em limit 1 for update skip locked",ExportacaoStore::mapear).stream().findFirst();
	}
	public void concluir(UUID id,String chave,String hashToken,Instant pronta,Instant expira){
		jdbc.update("update exportacao_dados set status='PRONTA',chave=?,hash_token=?,pronta_em=?,expira_em=? where id=?",chave,hashToken,Timestamp.from(pronta),Timestamp.from(expira),id);
	}
	public void falhou(UUID id,boolean definitivo){
		jdbc.update("update exportacao_dados set tentativas=tentativas+1,status=case when ? then 'FALHOU' else status end where id=?",definitivo,id);
	}
	public Optional<Pedido> porHashToken(String hash){
		return jdbc.query("select "+COLUNAS+" from exportacao_dados where hash_token=? and status='PRONTA'",ExportacaoStore::mapear,hash).stream().findFirst();
	}
	/** Pedidos prontos já vencidos: devolve as chaves para apagar o arquivo e marca como expirados. */
	public List<Pedido> expirar(Instant agora){
		var vencidos=jdbc.query("select "+COLUNAS+" from exportacao_dados where status='PRONTA' and expira_em<=? for update",ExportacaoStore::mapear,Timestamp.from(agora));
		for(var v:vencidos)jdbc.update("update exportacao_dados set status='EXPIRADA',hash_token=null,chave=null where id=?",v.id());
		return vencidos;
	}
}
