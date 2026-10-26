package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DiarioSyncBanco implements DiarioSync {
	private final JdbcTemplate jdbc;
	public DiarioSyncBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	@Override public long marcoAtual(){return jdbc.queryForObject("select ultimo_seq from estado_sync where id=1",Long.class);}
	@Override public List<ReferenciaSync> mudancasDesde(ColecaoSync colecao,long seq,ContextoUsuario usuario,int limite) {
		UUID destinatario=switch(usuario.perfil()){case NUTRICIONISTA->usuario.exigirNutricionista();case PACIENTE->usuario.exigirPaciente();default->throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);};
		return jdbc.query("""
			select seq,registro_id,nutricionista_id,paciente_id from (
			 select distinct on(registro_id) seq,registro_id,nutricionista_id,paciente_id from diario_sync
			 where perfil=? and destinatario_id=? and colecao=? and seq>? order by registro_id,seq desc
			) recentes order by seq limit ?
			""",(rs,n)->new ReferenciaSync(rs.getLong("seq"),rs.getObject("registro_id",UUID.class),rs.getObject("nutricionista_id",UUID.class),rs.getObject("paciente_id",UUID.class)),usuario.perfil().name(),destinatario,colecao.name(),seq,limite);
	}
}
