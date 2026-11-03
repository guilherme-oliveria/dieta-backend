package com.br.dietamais.sync.dominio;

import com.br.dietamais.comum.api.*;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ConteudoMutacao {
	private ConteudoMutacao(){}
	public static byte[] canonico(MutacaoSync mutacao,JsonMapper mapper){
		var raiz=mapper.createObjectNode();raiz.put("dominio","Dieta+/sync-mutacao/v1");raiz.put("entidade",mutacao.entidade());raiz.put("operacao",mutacao.operacao().name());raiz.put("id",mutacao.id().toString());
		if(mutacao.versaoBase()==null)raiz.putNull("versaoBase");else raiz.put("versaoBase",mutacao.versaoBase());
		raiz.set("dados",mutacao.dados()==null?mapper.nullNode():normalizar(mutacao.dados(),mapper));return mapper.writeValueAsString(raiz).getBytes(StandardCharsets.UTF_8);
	}
	private static JsonNode normalizar(JsonNode valor,JsonMapper mapper){
		if(valor.isObject()){
			var campos=new TreeMap<String,JsonNode>();valor.properties().forEach(e->campos.put(e.getKey(),e.getValue()));var objeto=mapper.createObjectNode();campos.forEach((chave,item)->objeto.set(chave,normalizar(item,mapper)));return objeto;
		}
		if(valor.isArray()){var array=mapper.createArrayNode();for(var item:valor)array.add(normalizar(item,mapper));return array;}
		if(valor.isNumber())return mapper.getNodeFactory().numberNode(valor.decimalValue().stripTrailingZeros());
		return valor;
	}
}
