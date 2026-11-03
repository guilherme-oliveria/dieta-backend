package com.br.dietamais.pacientes.infra;

import com.br.dietamais.pacientes.api.ConsultaCabecalhoRelatorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsultaCabecalhoRelatorioBanco implements ConsultaCabecalhoRelatorio {
	private final JdbcTemplate jdbc;
	public ConsultaCabecalhoRelatorioBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	@Override public Optional<Cabecalho> doVinculo(UUID vinculoId){
		return jdbc.query("select nome,nutricionista_id from ficha_paciente where vinculo_id=?",(rs,n)->new Cabecalho(rs.getString(1),rs.getObject(2,UUID.class)),vinculoId).stream().findFirst();
	}
}
