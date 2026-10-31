package com.br.dietamais.referencias.api;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Consulta em memória sobre uma lista de faixas. É o que o banco usa depois de carregar e o que os testes de cálculo usam com o mesmo CSV do Liquibase. */
public final class TabelaFaixas implements ConsultaReferencias {
	/** Caminho do CSV de referências do adulto, o mesmo que o changeset 030 carrega no banco. */
	public static final String CSV_ADULTO="db/changelog/dados/referencias-adulto.csv";
	private final Map<String,List<FaixaReferencia>> porTabela=new HashMap<>();
	private final String versao;

	public TabelaFaixas(List<FaixaReferencia> faixas){
		faixas.stream().sorted(Comparator.comparingInt(FaixaReferencia::ordem)).forEach(f->porTabela.computeIfAbsent(f.tabela(),k->new ArrayList<>()).add(f));
		versao=faixas.stream().map(FaixaReferencia::versaoReferencia).max(Comparator.naturalOrder()).orElse("sem-referencia");
	}
	public static TabelaFaixas doCsv(InputStream csv){
		try(var leitor=new BufferedReader(new InputStreamReader(csv,StandardCharsets.UTF_8))){
			var faixas=new ArrayList<FaixaReferencia>();
			leitor.readLine();
			for(String linha;(linha=leitor.readLine())!=null;){
				if(linha.isBlank())continue;
				String[] c=linha.split(";",-1);
				faixas.add(new FaixaReferencia(Integer.parseInt(c[0]),c[1],vazioNulo(c[2]),inteiro(c[3]),inteiro(c[4]),decimal(c[5]),Boolean.parseBoolean(c[6]),decimal(c[7]),Boolean.parseBoolean(c[8]),c[9],c[10],c[11]));
			}
			return new TabelaFaixas(faixas);
		}catch(IOException e){throw new UncheckedIOException(e);}
	}
	@Override public Optional<String> classificar(String tabela,String sexo,Integer idadeAnos,BigDecimal valor){
		return porTabela.getOrDefault(tabela,List.of()).stream().filter(f->f.contem(sexo,idadeAnos,valor)).map(FaixaReferencia::codigo).findFirst();
	}
	@Override public String versaoVigente(){return versao;}
	private static String vazioNulo(String s){return s.isEmpty()?null:s;}
	private static Integer inteiro(String s){return s.isEmpty()?null:Integer.valueOf(s);}
	private static BigDecimal decimal(String s){return s.isEmpty()?null:new BigDecimal(s);}
}
