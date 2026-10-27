package com.br.dietamais.alimentos.infra;

import java.util.List;
import java.util.UUID;
import com.br.dietamais.alimentos.api.GrupoAlimentoResposta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogoAlimentosStore {
	private final JdbcTemplate jdbc;
	public CatalogoAlimentosStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public List<GrupoAlimentoResposta> grupos(){
		return jdbc.query("""
			select id,nome,kcal_por_porcao,carboidratos_por_porcao,proteinas_por_porcao,gorduras_por_porcao
			from grupo_alimento where excluido_em is null order by nome,id
			""",(rs,n)->new GrupoAlimentoResposta(rs.getObject("id",UUID.class),rs.getString("nome"),
				rs.getBigDecimal("kcal_por_porcao"),rs.getBigDecimal("carboidratos_por_porcao"),
				rs.getBigDecimal("proteinas_por_porcao"),rs.getBigDecimal("gorduras_por_porcao")));
	}
}
