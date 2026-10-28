package com.br.dietamais.pacientes.infra;

import com.br.dietamais.comum.api.ConsultaNutricionistas;
import com.br.dietamais.pacientes.api.OcupacaoPlano;
import java.util.Collections;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcupacaoPlanoBanco implements OcupacaoPlano {
	private final JdbcTemplate jdbc;
	private final ConsultaNutricionistas nutricionistas;
	public OcupacaoPlanoBanco(JdbcTemplate jdbc,ConsultaNutricionistas nutricionistas){this.jdbc=jdbc;this.nutricionistas=nutricionistas;}
	@Override public int daOrganizacaoAtual() {return contar(nutricionistas.daOrganizacaoAtual());}
	@Override public int daOrganizacao(java.util.UUID organizacaoId) {return contar(nutricionistas.daOrganizacao(organizacaoId));}
	private int contar(java.util.List<java.util.UUID> ids) {
		if(ids.isEmpty())return 0;
		String parametros=String.join(",",Collections.nCopies(ids.size(),"?"));
		return jdbc.queryForObject("select count(*) from vinculo where status in ('CONVIDADO','ATIVO') and excluido_em is null and nutricionista_id in ("+parametros+")",Integer.class,ids.toArray());
	}
}
