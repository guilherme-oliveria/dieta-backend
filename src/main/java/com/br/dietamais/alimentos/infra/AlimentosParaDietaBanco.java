package com.br.dietamais.alimentos.infra;

import com.br.dietamais.alimentos.api.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlimentosParaDietaBanco implements ConsultarAlimentosParaDieta {
	private final JdbcTemplate jdbc;
	public AlimentosParaDietaBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	@Override public Map<UUID,AlimentoParaDieta> consultar(UUID nutri,Collection<UUID> ids){
		var resultado=new HashMap<UUID,AlimentoParaDieta>();if(ids.isEmpty())return resultado;
		jdbc.query("""
			select a.id,a.grupo_id,a.nome,a.medida_caseira,a.gramas_por_porcao,a.ativo,g.kcal_por_porcao,g.carboidratos_por_porcao,g.proteinas_por_porcao,g.gorduras_por_porcao
			from alimento_nutri a join grupo_alimento_nutri g on g.nutricionista_id=a.nutricionista_id and g.grupo_id=a.grupo_id
			where a.nutricionista_id=? and a.excluido_em is null and a.id=any(?)
			""",rs->{resultado.put(rs.getObject("id",UUID.class),new AlimentoParaDieta(rs.getObject("id",UUID.class),rs.getObject("grupo_id",UUID.class),rs.getString("nome"),
				rs.getString("medida_caseira"),rs.getBigDecimal("gramas_por_porcao"),rs.getBoolean("ativo"),rs.getBigDecimal("kcal_por_porcao"),
				rs.getBigDecimal("carboidratos_por_porcao"),rs.getBigDecimal("proteinas_por_porcao"),rs.getBigDecimal("gorduras_por_porcao")));},
			nutri,ids.toArray(new UUID[0]));
		return resultado;
	}
}
