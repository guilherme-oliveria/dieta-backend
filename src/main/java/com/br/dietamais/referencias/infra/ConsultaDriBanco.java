package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_dri} uma vez (só muda por changeset) e consulta em memória. */
@Component
class ConsultaDriBanco implements ConsultaDri {
	private final JdbcTemplate jdbc;
	private volatile TabelaDri tabela;
	ConsultaDriBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private TabelaDri tabela(){
		var t=tabela;
		if(t==null){
			synchronized(this){
				if(tabela==null)tabela=new TabelaDri(jdbc.query("select * from referencia_dri order by ordem",(rs,i)->new TabelaDri.Linha(rs.getInt("ordem"),rs.getString("nutriente"),rs.getString("nome"),
					rs.getString("sexo"),rs.getBoolean("gestante"),rs.getBoolean("lactante"),rs.getInt("idade_min_meses"),(Integer)rs.getObject("idade_max_meses"),rs.getBigDecimal("valor"),rs.getString("unidade"))));
				t=tabela;
			}
		}
		return t;
	}
	@Override public List<ValorDri> metas(String sexo,int idadeMeses,boolean gestante,boolean lactante){return tabela().metas(sexo,idadeMeses,gestante,lactante);}
	@Override public Optional<ValorDri> meta(String nutriente,String sexo,int idadeMeses,boolean gestante,boolean lactante){return tabela().meta(nutriente,sexo,idadeMeses,gestante,lactante);}
}
