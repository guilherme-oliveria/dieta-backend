package com.br.dietamais.referencias.api;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Consulta em memória das atividades de MET: o banco usa depois de carregar e os testes usam o mesmo CSV do Liquibase. */
public final class TabelaAtividadesMet implements ConsultaAtividadesMet {
	/** Caminho do CSV que o changeset 033 carrega no banco. */
	public static final String CSV="db/changelog/dados/referencia-met.csv";
	private final List<AtividadeMet> atividades;
	private final Map<String,AtividadeMet> porCodigo=new HashMap<>();

	public TabelaAtividadesMet(List<AtividadeMet> atividades){
		this.atividades=atividades.stream().sorted(Comparator.comparingInt(AtividadeMet::ordem)).toList();
		this.atividades.forEach(a->porCodigo.put(a.codigo(),a));
	}
	public static TabelaAtividadesMet doCsv(InputStream csv){
		try(var leitor=new BufferedReader(new InputStreamReader(csv,StandardCharsets.UTF_8))){
			var lista=new ArrayList<AtividadeMet>();
			leitor.readLine();
			for(String linha;(linha=leitor.readLine())!=null;){
				if(linha.isBlank())continue;
				String[] c=linha.split(";",-1);
				lista.add(new AtividadeMet(Integer.parseInt(c[0]),c[1],c[2],new BigDecimal(c[3]),c[4],c[5]));
			}
			return new TabelaAtividadesMet(lista);
		}catch(IOException e){throw new UncheckedIOException(e);}
	}
	@Override public List<AtividadeMet> listar(){return atividades;}
	@Override public Optional<AtividadeMet> buscar(String codigo){return Optional.ofNullable(porCodigo.get(codigo));}
}
