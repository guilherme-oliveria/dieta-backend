package com.br.dietamais.rotina.infra;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SemCheckinStore {
	public record Parado(UUID nutricionistaId,UUID pacienteId,LocalDate ultimaAtividade) {}
	private final JdbcTemplate jdbc;
	public SemCheckinStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	/**
	 * Pacientes de vínculo ativo com dieta da própria nutri em uso cuja última atividade (check-in ou início do uso da dieta)
	 * é de {@code dias} dias ou mais atrás, na data local do paciente.
	 */
	public List<Parado> parados(Instant agora,int dias){
		return jdbc.query("""
			select v.nutricionista_id,v.paciente_id,ult.d
			from vinculo v
			join paciente p on p.id=v.paciente_id
			join usuario u on u.id=p.usuario_id
			join dieta_em_uso du on du.paciente_id=v.paciente_id
			join dieta d on d.id=du.dieta_id and d.nutricionista_id=v.nutricionista_id
			cross join lateral (select greatest(coalesce(max(c.data),date '-infinity'),(du.atualizado_em at time zone u.fuso)::date) as d
				from checkin c where c.paciente_id=v.paciente_id and c.vinculo_id=v.id) ult
			where v.status='ATIVO' and ult.d<=((?::timestamptz at time zone u.fuso)::date-?)
			""",(rs,i)->new Parado(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,LocalDate.class)),Timestamp.from(agora),dias);
	}
}
