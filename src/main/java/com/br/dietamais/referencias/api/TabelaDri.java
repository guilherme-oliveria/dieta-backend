package com.br.dietamais.referencias.api;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Consulta em memória das DRI: o banco usa depois de carregar e os testes usam o mesmo CSV do Liquibase. */
public final class TabelaDri implements ConsultaDri {
	/** Caminho do CSV que o changeset 034 carrega no banco. */
	public static final String CSV="db/changelog/dados/referencia-dri.csv";
	public record Linha(int ordem,String nutriente,String nome,String sexo,boolean gestante,boolean lactante,int idadeMinMeses,Integer idadeMaxMeses,BigDecimal valor,String unidade){
		boolean contem(String s,int meses,boolean g,boolean l){
			return lactante==l&&gestante==g&&(sexo==null||sexo.equals(s))&&meses>=idadeMinMeses&&(idadeMaxMeses==null||meses<idadeMaxMeses);
		}
	}
	private final List<Linha> linhas;
	private final List<String> nutrientes;

	public TabelaDri(List<Linha> linhas){
		this.linhas=linhas.stream().sorted(Comparator.comparingInt(Linha::ordem)).toList();
		this.nutrientes=this.linhas.stream().map(Linha::nutriente).distinct().toList();
	}
	public static TabelaDri doCsv(InputStream csv){
		try(var leitor=new BufferedReader(new InputStreamReader(csv,StandardCharsets.UTF_8))){
			var lista=new ArrayList<Linha>();
			leitor.readLine();
			for(String linha;(linha=leitor.readLine())!=null;){
				if(linha.isBlank())continue;
				String[] c=linha.split(";",-1);
				lista.add(new Linha(Integer.parseInt(c[0]),c[1],c[2],c[3].equals("NULL")?null:c[3],Boolean.parseBoolean(c[4]),Boolean.parseBoolean(c[5]),Integer.parseInt(c[6]),
					c[7].equals("NULL")?null:Integer.valueOf(c[7]),new BigDecimal(c[8]),c[9]));
			}
			return new TabelaDri(lista);
		}catch(IOException e){throw new UncheckedIOException(e);}
	}
	@Override public List<ValorDri> metas(String sexo,int idadeMeses,boolean gestante,boolean lactante){
		var r=new ArrayList<ValorDri>();
		for(var n:nutrientes)meta(n,sexo,idadeMeses,gestante,lactante).ifPresent(r::add);
		return r;
	}
	@Override public Optional<ValorDri> meta(String nutriente,String sexo,int idadeMeses,boolean gestante,boolean lactante){
		boolean l=lactante,g=!lactante&&gestante;
		return linhas.stream().filter(x->x.nutriente().equals(nutriente)&&x.contem(sexo,idadeMeses,g,l)).findFirst()
			.map(x->new ValorDri(x.nutriente(),x.nome(),x.unidade(),x.valor()));
	}
}
