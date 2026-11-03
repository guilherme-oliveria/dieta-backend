package com.br.dietamais.alimentos.infra;

import java.util.List;
import java.util.UUID;
import com.br.dietamais.alimentos.api.GrupoAlimentoResposta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.br.dietamais.alimentos.api.*;
import java.sql.*;
import java.time.Instant;

@Repository
public class CatalogoAlimentosStore {
	private final JdbcTemplate jdbc;
	public CatalogoAlimentosStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public List<GrupoAlimentoResposta> grupos(UUID nutri){
		return jdbc.query("select * from grupo_alimento_nutri where nutricionista_id=? and excluido_em is null order by nome,grupo_id",this::mapear,nutri);
	}
	public GrupoAlimentoResposta grupo(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query("select * from grupo_alimento_nutri where grupo_id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	public void atualizar(UUID id,UUID nutri,UUID usuario,AtualizarMacrosGrupoRequisicao dados,Instant agora){
		jdbc.update("""
			update grupo_alimento_nutri set carboidratos_por_porcao=?,proteinas_por_porcao=?,gorduras_por_porcao=?,
			origem_macros='AJUSTE_NUTRI',estimativa=null,ajustado_por=?,ajustado_em=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=?
			where grupo_id=? and nutricionista_id=? and excluido_em is null
			""",dados.carboidratosPorPorcao(),dados.proteinasPorPorcao(),dados.gordurasPorPorcao(),usuario,Timestamp.from(agora),Timestamp.from(agora),id,nutri);
	}
	private GrupoAlimentoResposta mapear(ResultSet rs,int linha) throws SQLException {
		return new GrupoAlimentoResposta(rs.getObject("grupo_id",UUID.class),rs.getString("nome"),rs.getBigDecimal("kcal_por_porcao"),rs.getBigDecimal("carboidratos_por_porcao"),
			rs.getBigDecimal("proteinas_por_porcao"),rs.getBigDecimal("gorduras_por_porcao"),rs.getObject("estimativa",Boolean.class),rs.getLong("versao"),OrigemMacros.valueOf(rs.getString("origem_macros")));
	}
}
