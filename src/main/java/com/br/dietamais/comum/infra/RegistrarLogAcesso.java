package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.AcessoClinico;
import java.sql.Timestamp;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class RegistrarLogAcesso {
	private final JdbcTemplate jdbc;
	public RegistrarLogAcesso(JdbcTemplate jdbc){this.jdbc=jdbc;}
	@EventListener
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void registrar(AcessoClinico evento) {
		var recurso=evento.recurso();
		jdbc.update("""
			insert into log_acesso(usuario_id,nutricionista_id,paciente_id,acao,entidade,registro_id,ocorrido_em,origem_ip,trace_id,perfil,organizacao_id)
			values(?,?,?,?,?,?,?,cast(? as inet),?,?,?)
			""",evento.usuarioId(),recurso.nutricionistaId(),recurso.pacienteId(),evento.acao(),recurso.entidade(),recurso.registroId(),
				Timestamp.from(evento.ocorridoEm()),evento.origemIp(),evento.traceId(),evento.perfil().name(),recurso.organizacaoId());
	}
}
