package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_exame} e {@code referencia_exame_regra} uma vez (só mudam por changeset) e consulta em memória. */
@Component
class ConsultaExamesBanco implements ConsultaExames {
	private final JdbcTemplate jdbc;
	private volatile TabelaExames tabela;
	ConsultaExamesBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private TabelaExames tabela(){
		var t=tabela;
		if(t==null){
			synchronized(this){
				if(tabela==null){
					var cat=jdbc.query("select * from referencia_exame order by faixa_etaria, ordem",(rs,i)->{
						var c=rs.getString("condicoes");
						return new TabelaExames.LinhaCatalogo(rs.getString("faixa_etaria"),new ExameCatalogo(rs.getString("codigo"),rs.getString("secao"),rs.getString("nome"),rs.getString("unidade"),
							rs.getBoolean("qualitativo"),c==null||c.isBlank()?List.of():List.of(c.split("\\|"))));
					});
					var reg=jdbc.query("select * from referencia_exame_regra order by faixa_etaria, exame_codigo, ordem",(rs,i)->new TabelaExames.LinhaRegra(rs.getString("faixa_etaria"),rs.getString("exame_codigo"),
						new RegraExame(rs.getInt("ordem"),rs.getString("sexo"),rs.getString("condicao"),rs.getBigDecimal("idade_min"),rs.getBoolean("idade_min_incl"),rs.getBigDecimal("idade_max"),rs.getBoolean("idade_max_incl"),
							rs.getBigDecimal("valor_min"),rs.getBoolean("valor_min_incl"),rs.getBigDecimal("valor_max"),rs.getBoolean("valor_max_incl"),rs.getString("texto_igual"),rs.getString("rotulo"))));
					tabela=new TabelaExames(cat,reg);
				}
				t=tabela;
			}
		}
		return t;
	}
	@Override public List<String> faixasEtarias(){return tabela().faixasEtarias();}
	@Override public List<ExameCatalogo> catalogo(String faixaEtaria){return tabela().catalogo(faixaEtaria);}
	@Override public Optional<ExameCatalogo> exame(String faixaEtaria,String codigo){return tabela().exame(faixaEtaria,codigo);}
	@Override public Optional<ClassificacaoExame> classificar(String faixaEtaria,String codigo,BigDecimal valor,String texto,String sexo,Double idadeAnos,String condicao){
		return tabela().classificar(faixaEtaria,codigo,valor,texto,sexo,idadeAnos,condicao);
	}
}
