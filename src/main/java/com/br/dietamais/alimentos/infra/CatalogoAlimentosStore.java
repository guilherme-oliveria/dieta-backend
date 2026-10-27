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
			select g.id,g.nome,g.kcal_por_porcao,g.carboidratos_por_porcao,g.proteinas_por_porcao,g.gorduras_por_porcao,f.estimativa
			from grupo_alimento g left join fonte_macros_grupo f on f.grupo_id=g.id
			where g.excluido_em is null order by g.nome,g.id
			""",(rs,n)->new GrupoAlimentoResposta(rs.getObject("id",UUID.class),rs.getString("nome"),
				rs.getBigDecimal("kcal_por_porcao"),rs.getBigDecimal("carboidratos_por_porcao"),
				rs.getBigDecimal("proteinas_por_porcao"),rs.getBigDecimal("gorduras_por_porcao"),rs.getObject("estimativa",Boolean.class)));
	}
}
