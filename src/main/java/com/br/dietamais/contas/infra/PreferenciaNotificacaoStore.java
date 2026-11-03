package com.br.dietamais.contas.infra;

import com.br.dietamais.contas.api.SilencioNotificacao;
import com.br.dietamais.contas.api.TipoNotificacao;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class PreferenciaNotificacaoStore {
	public record Linha(List<TipoNotificacao> desligados,SilencioNotificacao silencio,long versao) {}
	private final JdbcTemplate jdbc;private final JsonMapper mapper;
	public PreferenciaNotificacaoStore(JdbcTemplate jdbc,JsonMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
	public Linha consultar(UUID usuario,boolean bloquear){
		return jdbc.query("select tipos_desligados,silencio_ativo,silencio_inicio,silencio_fim,versao from preferencia_notificacao where usuario_id=?"+(bloquear?" for update":""),
			(rs,i)->new Linha(mapper.readValue(rs.getString(1),new TypeReference<List<TipoNotificacao>>(){}),new SilencioNotificacao(rs.getBoolean(2),rs.getTime(3).toLocalTime(),rs.getTime(4).toLocalTime()),rs.getLong(5)),usuario)
			.stream().findFirst().orElse(null);
	}
	/** Cria a linha na versão 0 (padrões) se faltar, para poder bloqueá-la. */
	public void garantir(UUID usuario,Instant agora){
		jdbc.update("insert into preferencia_notificacao(usuario_id,atualizado_em) values(?,?) on conflict(usuario_id) do nothing",usuario,Timestamp.from(agora));
	}
	public void gravar(UUID usuario,List<TipoNotificacao> desligados,SilencioNotificacao s,Instant agora){
		jdbc.update("update preferencia_notificacao set tipos_desligados=?::jsonb,silencio_ativo=?,silencio_inicio=?,silencio_fim=?,versao=versao+1,atualizado_em=? where usuario_id=?",
			mapper.writeValueAsString(desligados),s.ativo(),Time.valueOf(s.inicio()),Time.valueOf(s.fim()),Timestamp.from(agora),usuario);
	}
}
