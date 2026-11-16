package com.br.dietamais.agenda.infra;

import com.br.dietamais.agenda.api.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FinanceiroStore {
	private final JdbcTemplate jdbc;
	public FinanceiroStore(JdbcTemplate jdbc){this.jdbc=jdbc;}

	public FinanceiroConsultaResposta da(UUID consulta,UUID nutri,boolean bloquear){
		return jdbc.query("select consulta_id,comparecimento,valor,forma_pagamento,pago,versao from consulta_financeiro where consulta_id=? and nutricionista_id=?"+(bloquear?" for update":""),this::mapear,consulta,nutri).stream().findFirst().orElse(null);
	}
	public void inserir(UUID consulta,UUID nutri,AtualizarFinanceiroConsultaRequisicao r,Instant agora){
		jdbc.update("insert into consulta_financeiro(consulta_id,nutricionista_id,comparecimento,valor,forma_pagamento,pago,criado_em,atualizado_em) values(?,?,?,?,?,?,?,?)",
			consulta,nutri,r.comparecimento().name(),r.valor(),r.formaPagamento()==null?null:r.formaPagamento().name(),r.pago(),Timestamp.from(agora),Timestamp.from(agora));
	}
	public void atualizar(UUID consulta,UUID nutri,AtualizarFinanceiroConsultaRequisicao r,Instant agora){
		jdbc.update("update consulta_financeiro set comparecimento=?,valor=?,forma_pagamento=?,pago=?,versao=versao+1,atualizado_em=? where consulta_id=? and nutricionista_id=?",
			r.comparecimento().name(),r.valor(),r.formaPagamento()==null?null:r.formaPagamento().name(),r.pago(),Timestamp.from(agora),consulta,nutri);
	}
	/** Consultas não canceladas da janela, com o financeiro quando houver (sem lançamento = PENDENTE, sem valor). */
	public ResumoFinanceiroResposta resumo(UUID nutri,Instant de,Instant ate){
		return jdbc.query("""
			select count(*) as consultas,
			  count(*) filter (where f.comparecimento='COMPARECEU') as compareceram,
			  count(*) filter (where f.comparecimento='FALTOU') as faltaram,
			  count(*) filter (where f.consulta_id is null or f.comparecimento='PENDENTE') as pendentes,
			  coalesce(sum(f.valor) filter (where f.pago),0) as recebido,
			  coalesce(sum(f.valor) filter (where not f.pago and f.comparecimento='COMPARECEU'),0) as a_receber
			from consulta c left join consulta_financeiro f on f.consulta_id=c.id
			where c.nutricionista_id=? and c.excluido_em is null and c.status<>'CANCELADA' and c.inicio>=? and c.inicio<?
			""",rs->{
				rs.next();
				var porForma=new EnumMap<FormaPagamento,BigDecimal>(FormaPagamento.class);
				jdbc.query("select f.forma_pagamento, sum(f.valor) as total from consulta c join consulta_financeiro f on f.consulta_id=c.id where c.nutricionista_id=? and c.excluido_em is null and c.status<>'CANCELADA' and c.inicio>=? and c.inicio<? and f.pago group by f.forma_pagamento",
					r2->{porForma.put(FormaPagamento.valueOf(r2.getString("forma_pagamento")),r2.getBigDecimal("total"));},nutri,Timestamp.from(de),Timestamp.from(ate));
				return new ResumoFinanceiroResposta(de,ate,rs.getInt("consultas"),rs.getInt("compareceram"),rs.getInt("faltaram"),rs.getInt("pendentes"),rs.getBigDecimal("recebido"),rs.getBigDecimal("a_receber"),porForma);
			},nutri,Timestamp.from(de),Timestamp.from(ate));
	}
	private FinanceiroConsultaResposta mapear(ResultSet rs,int linha) throws SQLException {
		var forma=rs.getString("forma_pagamento");
		return new FinanceiroConsultaResposta(rs.getObject("consulta_id",UUID.class),ComparecimentoConsulta.valueOf(rs.getString("comparecimento")),rs.getBigDecimal("valor"),forma==null?null:FormaPagamento.valueOf(forma),rs.getBoolean("pago"),rs.getLong("versao"));
	}
}
