package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_frisancho} uma vez (só muda por changeset, 80 linhas) e consulta em memória. */
@Component
class ConsultaFrisanchoBanco implements ConsultaFrisancho {
	private final JdbcTemplate jdbc;
	private volatile Map<String,PercentisFrisancho> tabela;
	ConsultaFrisanchoBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private Map<String,PercentisFrisancho> tabela(){
		var t=tabela;
		if(t==null){
			synchronized(this){
				if(tabela==null){
					var m=new HashMap<String,PercentisFrisancho>();
					jdbc.query("select * from referencia_frisancho",rs->{
						var p=new PercentisFrisancho(rs.getString("indicador"),rs.getString("sexo"),rs.getInt("idade_anos"),rs.getBigDecimal("p5"),rs.getBigDecimal("p10"),rs.getBigDecimal("p50"),rs.getBigDecimal("p90"));
						m.put(p.indicador()+"|"+p.sexo()+"|"+p.idadeAnos(),p);
					});
					tabela=m;
				}
				t=tabela;
			}
		}
		return t;
	}
	@Override public Optional<PercentisFrisancho> percentis(String indicador,String sexo,int idadeAnos){return Optional.ofNullable(tabela().get(indicador+"|"+sexo+"|"+idadeAnos));}
}
