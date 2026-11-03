package com.br.dietamais.agenda.infra;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CalendarioStore {
	public record Dono(UUID pacienteId,String fuso) {}
	private final JdbcTemplate jdbc;
	public CalendarioStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	/** Um token por paciente: gerar outro substitui (revoga) o anterior. */
	public void gravar(UUID paciente,String hash,Instant agora){
		jdbc.update("insert into calendario_token(paciente_id,token_hash,criado_em) values(?,?,?) on conflict(paciente_id) do update set token_hash=excluded.token_hash,criado_em=excluded.criado_em",paciente,hash,Timestamp.from(agora));
	}
	public boolean revogar(UUID paciente){return jdbc.update("delete from calendario_token where paciente_id=?",paciente)>0;}
	public Optional<Dono> porHash(String hash){
		return jdbc.query("select t.paciente_id,u.fuso from calendario_token t join paciente p on p.id=t.paciente_id join usuario u on u.id=p.usuario_id where t.token_hash=?",
			(rs,i)->new Dono(rs.getObject(1,UUID.class),rs.getString(2)),hash).stream().findFirst();
	}
}
