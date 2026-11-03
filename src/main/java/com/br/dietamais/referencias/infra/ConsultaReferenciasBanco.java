package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_faixa} uma vez (as tabelas só mudam por changeset, ou seja, a cada release) e consulta em memória. */
@Component
class ConsultaReferenciasBanco implements ConsultaReferencias {
	private final JdbcTemplate jdbc;
	private volatile TabelaFaixas tabelas;
	ConsultaReferenciasBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private TabelaFaixas tabelas(){
		var t=tabelas;
		if(t==null){
			synchronized(this){
				if(tabelas==null)tabelas=new TabelaFaixas(jdbc.query("select * from referencia_faixa order by ordem",(rs,i)->new FaixaReferencia(rs.getInt("ordem"),rs.getString("tabela"),
					rs.getString("sexo"),(Integer)rs.getObject("idade_min"),(Integer)rs.getObject("idade_max"),rs.getBigDecimal("valor_min"),rs.getBoolean("min_inclusivo"),
					rs.getBigDecimal("valor_max"),rs.getBoolean("max_inclusivo"),rs.getString("codigo"),rs.getString("fonte"),rs.getString("versao_referencia"))));
				t=tabelas;
			}
		}
		return t;
	}
	@Override public Optional<String> classificar(String tabela,String sexo,Integer idadeAnos,BigDecimal valor){return tabelas().classificar(tabela,sexo,idadeAnos,valor);}
	@Override public String versaoVigente(){return tabelas().versaoVigente();}
}
