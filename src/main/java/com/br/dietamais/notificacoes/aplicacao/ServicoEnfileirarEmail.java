package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.comum.api.GeradorIds;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.notificacoes.infra.CifradorEmail;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ServicoEnfileirarEmail implements EnfileirarEmail {
	private final JdbcTemplate jdbc;
	private final CifradorEmail cifra;
	private final JsonMapper mapper;
	private final GeradorIds ids;
	private final Clock relogio;
	public ServicoEnfileirarEmail(JdbcTemplate jdbc,CifradorEmail cifra,JsonMapper mapper,GeradorIds ids,Clock relogio) {
		this.jdbc=jdbc;this.cifra=cifra;this.mapper=mapper;this.ids=ids;this.relogio=relogio;
	}
	@Override @Transactional(propagation=Propagation.MANDATORY)
	public void enfileirar(EmailTransacional email) {
		var id=ids.novo();var agora=relogio.instant();
		var payload=cifra.cifrar(id,email.tipo().name(),mapper.writeValueAsString(email));
		jdbc.update("insert into notificacao_pendente(id,tipo,payload_cifrado,criado_em,expira_em,proxima_tentativa_em) values(?,?,?,?,?,?)",
				id,email.tipo().name(),payload,Timestamp.from(agora),Timestamp.from(email.expiraEm()),Timestamp.from(agora));
	}
}
