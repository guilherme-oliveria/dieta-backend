package com.br.dietamais.avaliacoes.infra;

import com.br.dietamais.avaliacoes.api.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProtocolosNutriStore {
	private final JdbcTemplate jdbc;
	public ProtocolosNutriStore(JdbcTemplate jdbc){this.jdbc=jdbc;}

	/** Nulo se a nutri nunca salvou protocolos. */
	public ProtocolosNutriResposta consultar(UUID nutri,boolean bloquear){
		return jdbc.query("select dobras,massa,versao,atualizado_em from protocolo_nutri where nutricionista_id=?"+(bloquear?" for update":""),
			(rs,n)->new ProtocolosNutriResposta(ProtocoloDobras.valueOf(rs.getString("dobras")),ProtocoloMassa.valueOf(rs.getString("massa")),rs.getLong("versao"),rs.getTimestamp("atualizado_em").toInstant()),nutri)
			.stream().findFirst().orElse(null);
	}
	/** Primeira gravação (versão 0 → 1). Falso se outra requisição chegou antes. */
	public boolean inserir(UUID nutri,ProtocoloDobras dobras,ProtocoloMassa massa,Instant agora){
		return jdbc.update("insert into protocolo_nutri(nutricionista_id,dobras,massa,versao,criado_em,atualizado_em) values(?,?,?,1,?,?) on conflict(nutricionista_id) do nothing",
			nutri,dobras.name(),massa.name(),Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public boolean atualizar(UUID nutri,ProtocoloDobras dobras,ProtocoloMassa massa,long versaoAnterior,Instant agora){
		return jdbc.update("update protocolo_nutri set dobras=?,massa=?,versao=versao+1,atualizado_em=? where nutricionista_id=? and versao=?",
			dobras.name(),massa.name(),Timestamp.from(agora),nutri,versaoAnterior)==1;
	}
}
