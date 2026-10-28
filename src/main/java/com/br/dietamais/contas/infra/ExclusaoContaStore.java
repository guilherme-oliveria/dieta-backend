package com.br.dietamais.contas.infra;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExclusaoContaStore {
	private final JdbcTemplate jdbc;
	public ExclusaoContaStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public boolean temPerfilNutri(UUID usuario){return jdbc.queryForObject("select exists(select 1 from nutricionista where usuario_id=?)",Boolean.class,usuario);}
	/** Tira o acesso e a identidade da conta; o registro fica só como referência do consentimento e dos logs de acesso (retenção legal). */
	public void anonimizarConta(UUID usuario,Instant agora){
		jdbc.update("update usuario set nome='Titular removido',email=?,senha_hash='!conta-excluida',ativo=false,atualizado_em=? where id=?","removido-"+usuario+"@removido.invalid",Timestamp.from(agora),usuario);
		jdbc.update("delete from refresh_token where usuario_id=?",usuario);
		jdbc.update("delete from dispositivo where usuario_id=?",usuario);
		jdbc.update("delete from preferencia_notificacao where usuario_id=?",usuario);
		jdbc.update("delete from token_uso_unico where usuario_id=?",usuario);
	}
	/** Login que também é de nutri: encerra só as sessões do perfil paciente. */
	public void revogarSessoesPaciente(UUID usuario){jdbc.update("delete from refresh_token where usuario_id=? and perfil='PACIENTE'",usuario);}
}
