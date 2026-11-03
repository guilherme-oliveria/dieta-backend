package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_met} uma vez (só muda por changeset) e consulta em memória. */
@Component
class ConsultaAtividadesMetBanco implements ConsultaAtividadesMet {
	private final JdbcTemplate jdbc;
	private volatile TabelaAtividadesMet tabela;
	ConsultaAtividadesMetBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private TabelaAtividadesMet tabela(){
		var t=tabela;
		if(t==null){
			synchronized(this){
				if(tabela==null)tabela=new TabelaAtividadesMet(jdbc.query("select * from referencia_met order by ordem",(rs,i)->new AtividadeMet(rs.getInt("ordem"),rs.getString("codigo"),
					rs.getString("nome"),rs.getBigDecimal("met"),rs.getString("fonte"),rs.getString("versao_referencia"))));
				t=tabela;
			}
		}
		return t;
	}
	@Override public List<AtividadeMet> listar(){return tabela().listar();}
	@Override public Optional<AtividadeMet> buscar(String codigo){return tabela().buscar(codigo);}
}
