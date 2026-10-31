package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.infra.ProtocolosNutriStore;
import com.br.dietamais.comum.api.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** "Meus protocolos" (BE-42, RN-CAL-03): padrão da nutri para avaliações novas; quem nunca salvou usa {@code SETE}/{@code MAGRO} na versão 0. */
@Service
public class GerenciarProtocolosNutri {
	private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;private final ProtocolosNutriStore store;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarProtocolosNutri(UsuarioAtual usuario,GuardaEscritaNutri guarda,ProtocolosNutriStore store,Clock relogio,JsonMapper mapper){
		this.usuario=usuario;this.guarda=guarda;this.store=store;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true)
	public ProtocolosNutriResposta consultar(){return efetivos(usuario.obter().exigirNutricionista(),false);}

	/** Usado pelo cálculo da avaliação quando a requisição não traz protocolo. */
	@Transactional(readOnly=true)
	public ProtocoloAvaliacao padrao(UUID nutri){
		var p=efetivos(nutri,false);return new ProtocoloAvaliacao(p.dobras(),p.massa(),null);
	}

	@Transactional
	public ProtocolosNutriResposta atualizar(String ifMatch,SalvarProtocolosNutriRequisicao r){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();
		var atual=efetivos(nutri,true);
		boolean gravou=versao(ifMatch)==atual.versao()&&(atual.versao()==0?store.inserir(nutri,r.dobras(),r.massa(),relogio.instant()):store.atualizar(nutri,r.dobras(),r.massa(),atual.versao(),relogio.instant()));
		if(!gravou)throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","Os protocolos foram atualizados. Revise os dados atuais.",List.of(),mapper.valueToTree(efetivos(nutri,false)));
		return efetivos(nutri,false);
	}
	private ProtocolosNutriResposta efetivos(UUID nutri,boolean bloquear){
		var salvo=store.consultar(nutri,bloquear);
		return salvo!=null?salvo:new ProtocolosNutriResposta(CalculoAvaliacao.DOBRAS_PADRAO,CalculoAvaliacao.MASSA_PADRAO,0L,Instant.EPOCH);
	}
	private static long versao(String valor){
		if(valor!=null&&valor.matches("\"[0-9]+\""))try{return Long.parseLong(valor.substring(1,valor.length()-1));}catch(NumberFormatException ignorada){}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02");
	}
}
