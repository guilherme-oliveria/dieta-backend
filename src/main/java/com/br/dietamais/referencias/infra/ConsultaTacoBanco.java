package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_taco} uma vez (só muda por changeset, 597 linhas) e consulta em memória. */
@Component
class ConsultaTacoBanco implements ConsultaTaco {
	private static final List<String> NUTRIENTES=List.of("umidade_g","energia_kcal","energia_kj","proteina_g","lipideos_g","colesterol_mg","carboidrato_g","fibra_g","cinzas_g",
		"calcio_mg","magnesio_mg","manganes_mg","fosforo_mg","ferro_mg","sodio_mg","potassio_mg","cobre_mg","zinco_mg","retinol_ug","re_ug","rae_ug","tiamina_mg","riboflavina_mg",
		"piridoxina_mg","niacina_mg","vitamina_c_mg","saturados_g","monoinsaturados_g","poliinsaturados_g");
	private final JdbcTemplate jdbc;
	private volatile List<Linha> linhas;
	ConsultaTacoBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private record Linha(AlimentoTaco alimento,String nomeNormalizado){}

	private List<Linha> linhas(){
		var l=linhas;
		if(l==null){
			synchronized(this){
				if(linhas==null)linhas=jdbc.query("select * from referencia_taco order by numero",(rs,i)->{
					var nutrientes=new LinkedHashMap<String,BigDecimal>();
					for(var n:NUTRIENTES){var v=rs.getBigDecimal(n);if(v!=null)nutrientes.put(n,v.stripTrailingZeros().scale()<0?v.setScale(0):v.stripTrailingZeros());}
					var traco=rs.getString("traco");
					var a=new AlimentoTaco(rs.getInt("numero"),rs.getString("nome"),rs.getString("categoria"),Collections.unmodifiableMap(nutrientes),
						traco==null||traco.isBlank()?List.of():List.of(traco.split(",")));
					return new Linha(a,normalizar(a.nome()));
				});
				l=linhas;
			}
		}
		return l;
	}
	static String normalizar(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}

	@Override public List<AlimentoTaco> buscar(String trecho,String categoria,int limite){
		var palavras=trecho==null||trecho.isBlank()?new String[0]:normalizar(trecho.trim()).split("[\\s,]+");
		return linhas().stream().filter(l->(categoria==null||categoria.equals(l.alimento().categoria()))&&Arrays.stream(palavras).allMatch(l.nomeNormalizado()::contains)).limit(limite).map(Linha::alimento).toList();
	}
	@Override public Optional<AlimentoTaco> porNumero(int numero){return linhas().stream().map(Linha::alimento).filter(a->a.numero()==numero).findFirst();}
	@Override public List<String> categorias(){return linhas().stream().map(l->l.alimento().categoria()).distinct().toList();}
}
