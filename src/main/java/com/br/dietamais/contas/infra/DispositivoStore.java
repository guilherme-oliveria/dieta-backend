package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.Plataforma;
import com.br.dietamais.contas.api.DispositivoResposta;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DispositivoStore {
	private static final int LIMITE_POR_USUARIO=10;
	private final JdbcTemplate jdbc;
	public DispositivoStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	/** O token é único: se já era de outro usuário (troca de conta no aparelho), passa para quem registrou agora. */
	public DispositivoResposta registrar(UUID usuario,String token,Plataforma plataforma,String nome,Instant agora){
		var r=jdbc.queryForObject("""
			insert into dispositivo(usuario_id,token,plataforma,nome_aparelho,criado_em,atualizado_em) values(?,?,?,?,?,?)
			on conflict(token) do update set usuario_id=excluded.usuario_id,plataforma=excluded.plataforma,nome_aparelho=excluded.nome_aparelho,atualizado_em=excluded.atualizado_em
			returning id,plataforma,nome_aparelho
			""",(rs,i)->new DispositivoResposta(rs.getObject(1,UUID.class),Plataforma.valueOf(rs.getString(2)),rs.getString(3)),usuario,token,plataforma.name(),nome,Timestamp.from(agora),Timestamp.from(agora));
		jdbc.update("delete from dispositivo where usuario_id=? and id not in (select id from dispositivo where usuario_id=? order by atualizado_em desc,id desc limit ?)",usuario,usuario,LIMITE_POR_USUARIO);
		return r;
	}
	public boolean remover(UUID usuario,UUID id){return jdbc.update("delete from dispositivo where id=? and usuario_id=?",id,usuario)>0;}
}
