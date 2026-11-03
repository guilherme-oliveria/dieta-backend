package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.*;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RegistroAdminBanco implements RegistroAdmin {
	private final JdbcTemplate jdbc;private final UsuarioAtual usuario;private final Clock relogio;private final JsonMapper mapper;private final HttpServletRequest requisicao;
	public RegistroAdminBanco(JdbcTemplate jdbc,UsuarioAtual usuario,Clock relogio,JsonMapper mapper,HttpServletRequest requisicao){
		this.jdbc=jdbc;this.usuario=usuario;this.relogio=relogio;this.mapper=mapper;this.requisicao=requisicao;
	}
	@Override public void registrar(String acao,String alvoTipo,UUID alvoId,Object antes,Object depois,String motivo){
		var contexto=usuario.obter();
		if(contexto.perfil()!=Perfil.ADMIN)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		if(motivo==null||motivo.isBlank())throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO,null);
		String ip=requisicao.getRemoteAddr();if(ip!=null&&ip.contains("%"))ip=ip.substring(0,ip.indexOf('%'));
		jdbc.update("insert into log_admin(admin_id,acao,alvo_tipo,alvo_id,antes,depois,motivo,ocorrido_em,origem_ip,trace_id) values(?,?,?,?,?::jsonb,?::jsonb,?,?,?::inet,?)",
			contexto.usuarioId(),acao,alvoTipo,alvoId,antes==null?null:mapper.writeValueAsString(antes),depois==null?null:mapper.writeValueAsString(depois),
			motivo.strip(),Timestamp.from(relogio.instant()),ip,MDC.get("traceId"));
	}
}
