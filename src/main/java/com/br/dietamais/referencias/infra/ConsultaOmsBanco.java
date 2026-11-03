package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_oms} uma vez (só muda por changeset, 736 linhas) e consulta em memória. */
@Component
class ConsultaOmsBanco implements ConsultaOms {
	private static final String[] P={"p0_1","p3","p10","p15","p50","p85","p97","p99"},Z={"z_m3","z_m2","z_m1","z_0","z_p1","z_p2","z_p3"};
	private final JdbcTemplate jdbc;
	private volatile Map<String,TreeMap<Integer,CurvaOms>> tabela;
	ConsultaOmsBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private Map<String,TreeMap<Integer,CurvaOms>> tabela(){
		var t=tabela;
		if(t==null){
			synchronized(this){
				if(tabela==null){
					var m=new HashMap<String,TreeMap<Integer,CurvaOms>>();
					jdbc.query("select * from referencia_oms",rs->{
						var p=new ArrayList<BigDecimal>();for(var c:P)p.add(rs.getBigDecimal(c));
						var z=new ArrayList<BigDecimal>();for(var c:Z)z.add(rs.getBigDecimal(c));
						var curva=new CurvaOms(rs.getString("indicador"),rs.getString("sexo"),rs.getInt("idade_meses"),List.copyOf(p),List.copyOf(z));
						m.computeIfAbsent(curva.indicador()+"|"+curva.sexo(),k->new TreeMap<>()).put(curva.idadeMeses(),curva);
					});
					tabela=m;
				}
				t=tabela;
			}
		}
		return t;
	}
	@Override public Optional<CurvaOms> curva(String indicador,String sexo,int idadeMeses){
		var linhas=tabela().get(indicador+"|"+sexo);
		if(linhas==null||idadeMeses<0)return Optional.empty();
		var e=linhas.floorEntry(idadeMeses);
		if(e==null)return Optional.empty();
		// mensal até 119 meses; depois só há uma linha por ano (IMC de 10 a 17 anos) e ela vale até o fim daquele ano
		if(idadeMeses>=120&&(!indicador.equals("IMC_IDADE")||idadeMeses-e.getKey()>11||e.getKey()<120))return Optional.empty();
		return Optional.of(e.getValue());
	}
}
