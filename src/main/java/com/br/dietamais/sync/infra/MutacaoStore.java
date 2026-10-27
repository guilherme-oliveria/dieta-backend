package com.br.dietamais.sync.infra;

import com.br.dietamais.comum.api.ResultadoMutacao;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class MutacaoStore {
	private final JdbcTemplate jdbc;private final JsonMapper mapper;
	public MutacaoStore(JdbcTemplate jdbc,JsonMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
	public record Guardada(UUID usuarioId,ResultadoMutacao resultado) {}
	public Guardada buscar(UUID idMutacao){
		return jdbc.query("select usuario_id,resultado::text from mutacao_sync where id_mutacao=?",(rs,n)->new Guardada(rs.getObject(1,UUID.class),mapper.readValue(rs.getString(2),ResultadoMutacao.class)),idMutacao).stream().findFirst().orElse(null);
	}
	public boolean guardar(UUID usuario,ResultadoMutacao resultado){
		return jdbc.update("insert into mutacao_sync(id_mutacao,usuario_id,resultado) values(?,?,?::jsonb) on conflict(id_mutacao) do nothing",resultado.idMutacao(),usuario,mapper.writeValueAsString(resultado))==1;
	}
}
