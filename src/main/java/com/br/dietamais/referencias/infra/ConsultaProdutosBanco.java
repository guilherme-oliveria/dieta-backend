package com.br.dietamais.referencias.infra;

import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lê {@code referencia_produto} uma vez (só muda por changeset, 400 linhas) e consulta em memória. */
@Component
class ConsultaProdutosBanco implements ConsultaProdutos {
	private final JdbcTemplate jdbc;
	private volatile List<Linha> linhas;
	ConsultaProdutosBanco(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private record Linha(ProdutoReferencia produto,String textoBusca){}

	private static String texto(String s){return s==null||s.isBlank()?null:s;}
	private static BigDecimal limpar(BigDecimal v){if(v==null)return null;var s=v.stripTrailingZeros();return s.scale()<0?s.setScale(0):s;}

	private List<Linha> linhas(){
		var l=linhas;
		if(l==null){
			synchronized(this){
				if(linhas==null){
					var nutrientes=new HashMap<Integer,Map<String,BigDecimal>>();
					jdbc.query("select produto,nutriente,valor from referencia_produto_nutriente order by produto,nutriente",rs->{
						nutrientes.computeIfAbsent(rs.getInt("produto"),k->new TreeMap<>()).put(rs.getString("nutriente"),limpar(rs.getBigDecimal("valor")));
					});
					linhas=jdbc.query("select * from referencia_produto order by numero",(rs,i)->{
						var n=rs.getInt("numero");
						var composicao=new ProdutoReferencia.Composicao(texto(rs.getString("carboidratos_composicao")),texto(rs.getString("proteinas_composicao")),
							texto(rs.getString("lipideos_composicao")),texto(rs.getString("fibras_composicao")));
						var diluicao=new ProdutoReferencia.Diluicao(limpar(rs.getBigDecimal("diluicao_medidas")),texto(rs.getString("diluicao_medida")),texto(rs.getString("diluicao_gramas")),
							texto(rs.getString("solvente_total")),texto(rs.getString("solvente")));
						var p=new ProdutoReferencia(n,rs.getString("nome"),texto(rs.getString("marca")),texto(rs.getString("classificacao")),texto(rs.getString("apresentacao")),
							texto(rs.getString("embalagem")),texto(rs.getString("sabores")),texto(rs.getString("medida_caseira")),limpar(rs.getBigDecimal("medida_g")),composicao,
							texto(rs.getString("relacao_kcal_n")),texto(rs.getString("osmolaridade")),texto(rs.getString("osmolalidade")),diluicao,texto(rs.getString("observacao")),
							Collections.unmodifiableMap(nutrientes.getOrDefault(n,Map.of())));
						return new Linha(p,ConsultaTacoBanco.normalizar(p.nome()+" "+(p.marca()==null?"":p.marca())));
					});
				}
				l=linhas;
			}
		}
		return l;
	}

	@Override public List<ProdutoReferencia> buscar(String trecho,String classificacao,String apresentacao,int limite){
		var palavras=trecho==null||trecho.isBlank()?new String[0]:ConsultaTacoBanco.normalizar(trecho.trim()).split("[\\s,]+");
		return linhas().stream().filter(l->(classificacao==null||classificacao.equals(l.produto().classificacao()))&&(apresentacao==null||apresentacao.equals(l.produto().apresentacao()))
			&&Arrays.stream(palavras).allMatch(l.textoBusca()::contains)).limit(limite).map(Linha::produto).toList();
	}
	@Override public Optional<ProdutoReferencia> porNumero(int numero){return linhas().stream().map(Linha::produto).filter(p->p.numero()==numero).findFirst();}
	@Override public List<String> classificacoes(){return linhas().stream().map(l->l.produto().classificacao()).filter(Objects::nonNull).distinct().sorted().toList();}
	@Override public List<String> apresentacoes(){return linhas().stream().map(l->l.produto().apresentacao()).filter(Objects::nonNull).distinct().sorted().toList();}
}
