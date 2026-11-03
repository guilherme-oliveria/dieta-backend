package com.br.dietamais.contas.infra;

import com.br.dietamais.contas.dominio.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TokenContaStore {
	private final JdbcTemplate jdbc;
	public TokenContaStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public void anular(UUID usuario,FinalidadeToken finalidade,Instant agora) {
		jdbc.update("update token_uso_unico set anulado_em=? where usuario_id=? and finalidade=? and usado_em is null and anulado_em is null",Timestamp.from(agora),usuario,finalidade.name());
	}
	public void guardar(UUID id,UUID usuario,FinalidadeToken finalidade,String hash,Instant agora,Instant fim) {
		jdbc.update("insert into token_uso_unico(id,usuario_id,finalidade,hash_token,criado_em,expira_em) values(?,?,?,?,?,?)",
				id,usuario,finalidade.name(),hash,Timestamp.from(agora),Timestamp.from(fim));
	}
	public Registro consultar(String hash) {
		return jdbc.query("select * from token_uso_unico where hash_token=?",(rs,n)->new Registro(rs.getObject("id",UUID.class),rs.getObject("usuario_id",UUID.class),
				new TokenUsoUnico(FinalidadeToken.valueOf(rs.getString("finalidade")),rs.getTimestamp("expira_em").toInstant(),rs.getTimestamp("usado_em")!=null,rs.getTimestamp("anulado_em")!=null)),hash).stream().findFirst().orElse(null);
	}
	public void consumir(UUID id,Instant agora){jdbc.update("update token_uso_unico set usado_em=? where id=?",Timestamp.from(agora),id);}
	public void limparExpirados(Instant agora){jdbc.update("delete from token_uso_unico where expira_em<=?",Timestamp.from(agora));}
	public void confirmarEmail(UUID usuario,Instant agora) {
		jdbc.update("update usuario set email_confirmado_em=coalesce(email_confirmado_em,?),atualizado_em=? where id=?",Timestamp.from(agora),Timestamp.from(agora),usuario);
	}
	public void redefinirSenha(UUID usuario,String hash,Instant agora) {
		jdbc.update("update usuario set senha_hash=?,atualizado_em=? where id=?",hash,Timestamp.from(agora),usuario);
		jdbc.update("update refresh_token set revogado_em=? where usuario_id=? and revogado_em is null",Timestamp.from(agora),usuario);
	}
	public record Registro(UUID id,UUID usuario,TokenUsoUnico estado) { }
}
