package com.br.dietamais.sync.infra;

import com.br.dietamais.comum.api.ResultadoMutacao;
import com.br.dietamais.comum.api.Perfil;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class MutacaoStore {
	private final JdbcTemplate jdbc;private final JsonMapper mapper;
	public MutacaoStore(JdbcTemplate jdbc,JsonMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
	public record Guardada(UUID usuarioId,Perfil perfil,String fingerprint,ResultadoMutacao resultado) {
		public Guardada(UUID usuarioId,ResultadoMutacao resultado){this(usuarioId,null,null,resultado);}
	}
	public void bloquear(UUID idMutacao){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"sync-mutacao:"+idMutacao);}
	public Guardada buscar(UUID idMutacao){
		return jdbc.query("select usuario_id,perfil,fingerprint,resultado::text from mutacao_sync where id_mutacao=?",(rs,n)->new Guardada(rs.getObject(1,UUID.class),rs.getString(2)==null?null:Perfil.valueOf(rs.getString(2)),rs.getString(3),mapper.readValue(rs.getString(4),ResultadoMutacao.class)),idMutacao).stream().findFirst().orElse(null);
	}
	public boolean guardar(UUID usuario,Perfil perfil,String fingerprint,ResultadoMutacao resultado){
		return jdbc.update("insert into mutacao_sync(id_mutacao,usuario_id,perfil,fingerprint,resultado) values(?,?,?,?,?::jsonb) on conflict(id_mutacao) do nothing",resultado.idMutacao(),usuario,perfil.name(),fingerprint,mapper.writeValueAsString(resultado))==1;
	}
}
