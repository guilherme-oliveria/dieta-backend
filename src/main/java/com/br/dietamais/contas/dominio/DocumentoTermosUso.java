package com.br.dietamais.contas.dominio;

import java.time.Instant;
import java.util.*;

public record DocumentoTermosUso(String versao,String status,Instant aprovadoEm,Instant publicadoEm,String conteudo) {
	public DocumentoTermosUso {
		if(versao==null||versao.isBlank()||versao.length()>40||status==null||!Set.of("RASCUNHO","APROVADO","PUBLICADO").contains(status)||conteudo==null||conteudo.isBlank())
			throw new IllegalArgumentException("Documento de termos inválido");
	}
	public boolean aprovado(Instant agora){return !"RASCUNHO".equals(status)&&aprovadoEm!=null&&!aprovadoEm.isAfter(agora);}
	public boolean habilitado(Instant agora){return "PUBLICADO".equals(status)&&aprovado(agora)&&publicadoEm!=null&&!aprovadoEm.isAfter(publicadoEm)&&!publicadoEm.isAfter(agora);}
	public static DocumentoTermosUso ler(String markdown) {
		String texto=markdown.replace("\r\n","\n");int fim=texto.indexOf("\n---\n",4);
		if(!texto.startsWith("---\n")||fim<0)throw new IllegalArgumentException("Metadados dos termos ausentes");
		var campos=new HashMap<String,String>();
		for(String linha:texto.substring(4,fim).split("\n")) {
			int separador=linha.indexOf(':');if(separador<1)throw new IllegalArgumentException("Metadado de termos inválido");
			String chave=linha.substring(0,separador).strip(),valor=linha.substring(separador+1).strip();
			if(!Set.of("versao","status","aprovadoEm","publicadoEm").contains(chave)||campos.putIfAbsent(chave,valor)!=null)throw new IllegalArgumentException("Metadado de termos desconhecido ou duplicado");
		}
		if(campos.size()!=4)throw new IllegalArgumentException("Metadados dos termos incompletos");
		return new DocumentoTermosUso(campos.get("versao"),campos.get("status"),instante(campos.get("aprovadoEm")),instante(campos.get("publicadoEm")),texto.substring(fim+5));
	}
	private static Instant instante(String valor){return valor.isBlank()?null:Instant.parse(valor);}
}
