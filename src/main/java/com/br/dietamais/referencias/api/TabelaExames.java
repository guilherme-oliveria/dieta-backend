package com.br.dietamais.referencias.api;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Consulta em memória ao catálogo e às regras. É o que o banco usa depois de carregar e o que os testes usam com os mesmos CSVs do Liquibase. */
public final class TabelaExames implements ConsultaExames {
	public static final String CSV_CATALOGO="db/changelog/dados/referencia-exame.csv",CSV_REGRAS="db/changelog/dados/referencia-exame-regra.csv";
	private final Map<String,LinkedHashMap<String,ExameCatalogo>> catalogo=new LinkedHashMap<>();
	private final Map<String,List<RegraExame>> regras=new HashMap<>();

	public record LinhaCatalogo(String faixaEtaria,ExameCatalogo exame){}
	public record LinhaRegra(String faixaEtaria,String codigo,RegraExame regra){}

	public TabelaExames(List<LinhaCatalogo> linhas,List<LinhaRegra> regraLinhas){
		for(var l:linhas)catalogo.computeIfAbsent(l.faixaEtaria(),k->new LinkedHashMap<>()).put(l.exame().codigo(),l.exame());
		regraLinhas.stream().sorted(Comparator.comparingInt(l->l.regra().ordem())).forEach(l->regras.computeIfAbsent(l.faixaEtaria()+"|"+l.codigo(),k->new ArrayList<>()).add(l.regra()));
	}
	public static TabelaExames doCsv(InputStream catalogoCsv,InputStream regrasCsv){
		var linhas=new ArrayList<LinhaCatalogo>();var rs=new ArrayList<LinhaRegra>();
		for(var c:ler(catalogoCsv))linhas.add(new LinhaCatalogo(c[0],new ExameCatalogo(c[1],c[2],c[3],vazioNulo(c[4]),Boolean.parseBoolean(c[6]),c[7].isEmpty()?List.of():List.of(c[7].split("\\|")))));
		for(var c:ler(regrasCsv))rs.add(new LinhaRegra(c[0],c[1],new RegraExame(Integer.parseInt(c[2]),vazioNulo(c[3]),vazioNulo(c[4]),decimal(c[5]),Boolean.parseBoolean(c[6]),decimal(c[7]),Boolean.parseBoolean(c[8]),
			decimal(c[9]),Boolean.parseBoolean(c[10]),decimal(c[11]),Boolean.parseBoolean(c[12]),vazioNulo(c[13]),c[14])));
		return new TabelaExames(linhas,rs);
	}
	private static List<String[]> ler(InputStream csv){
		try(var leitor=new BufferedReader(new InputStreamReader(csv,StandardCharsets.UTF_8))){
			var r=new ArrayList<String[]>();leitor.readLine();
			for(String linha;(linha=leitor.readLine())!=null;)if(!linha.isBlank())r.add(linha.split(";",-1));
			return r;
		}catch(IOException e){throw new UncheckedIOException(e);}
	}
	@Override public List<String> faixasEtarias(){return List.copyOf(catalogo.keySet());}
	@Override public List<ExameCatalogo> catalogo(String faixaEtaria){return List.copyOf(catalogo.getOrDefault(faixaEtaria,new LinkedHashMap<>()).values());}
	@Override public Optional<ExameCatalogo> exame(String faixaEtaria,String codigo){return Optional.ofNullable(catalogo.getOrDefault(faixaEtaria,new LinkedHashMap<>()).get(codigo));}
	@Override public Optional<ClassificacaoExame> classificar(String faixaEtaria,String codigo,BigDecimal valor,String texto,String sexo,Double idadeAnos,String condicao){
		if(exame(faixaEtaria,codigo).isEmpty())return Optional.empty();
		var faltas=EnumSet.noneOf(RegraExame.Falta.class);
		for(var r:regras.getOrDefault(faixaEtaria+"|"+codigo,List.of())){
			var f=r.confere(sexo,idadeAnos,condicao,valor,texto);
			if(f==RegraExame.Falta.NENHUMA)return Optional.of(ClassificacaoExame.de(r.rotulo()));
			faltas.add(f);
		}
		if(faltas.contains(RegraExame.Falta.SEXO))return Optional.of(ClassificacaoExame.sem("FALTA_SEXO"));
		if(faltas.contains(RegraExame.Falta.CONDICAO))return Optional.of(ClassificacaoExame.sem("FALTA_CONDICAO"));
		if(faltas.contains(RegraExame.Falta.IDADE))return Optional.of(ClassificacaoExame.sem("FALTA_IDADE"));
		return Optional.of(ClassificacaoExame.sem("FORA_DAS_FAIXAS"));
	}
	private static String vazioNulo(String s){return s==null||s.isEmpty()?null:s;}
	private static BigDecimal decimal(String s){return s.isEmpty()?null:new BigDecimal(s);}
}
