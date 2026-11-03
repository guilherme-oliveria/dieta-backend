package com.br.dietamais.alimentos.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.dominio.*;
import com.br.dietamais.alimentos.infra.ListasAlimentosStore;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GerenciarListaPaciente {
	private final ListasAlimentosStore listas;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;
	private final GuardaEscritaNutri guarda;private final AuditoriaClinica auditoria;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarListaPaciente(ListasAlimentosStore listas,ConsultarFichaParaLista fichas,UsuarioAtual usuario,GuardaEscritaNutri guarda,AuditoriaClinica auditoria,Clock relogio,JsonMapper mapper){
		this.listas=listas;this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;this.auditoria=auditoria;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public ListaSubstituicaoResposta consultar(UUID paciente){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var ficha=fichas.consultar(paciente,false);
		var resposta=montar(ficha,ajuste(ficha,nutri,false),nutri);auditoria.leitura(referencia(ficha,contexto));return resposta;
	}
	@Transactional
	public ListaSubstituicaoResposta atualizar(UUID paciente,String ifMatch,AtualizarListaSubstituicaoRequisicao requisicao){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();listas.bloquear(nutri);
		var ficha=fichas.consultar(paciente,true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		var atual=ajuste(ficha,nutri,true);
		if(VersaoLista.ler(ifMatch)!=atual.versao()){
			auditoria.leitura(referencia(ficha,contexto));throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A lista foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(montar(ficha,atual,nutri)));
		}
		var dados=new DadosAjusteLista(requisicao.adicionados(),requisicao.removidos());
		if(!listas.referenciasProprias(nutri,dados.adicionados(),true)||!listas.referenciasProprias(nutri,dados.removidos(),false))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		listas.atualizarAjuste(ficha.vinculoId(),nutri,dados,relogio.instant());
		var resposta=montar(ficha,ajuste(ficha,nutri,false),nutri);auditoria.escrita(referencia(ficha,contexto));return resposta;
	}
	private AjusteLista ajuste(FichaParaLista ficha,UUID nutri,boolean bloquear){
		var ajuste=listas.ajuste(ficha.vinculoId(),nutri,bloquear);if(ajuste==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return ajuste;
	}
	private ListaSubstituicaoResposta montar(FichaParaLista ficha,AjusteLista ajuste,UUID nutri){return new ListaSubstituicaoResposta(ficha.pacienteId(),ajuste.versao(),listas.efetivos(ajuste,nutri),ajuste.adicionados(),ajuste.removidos());}
	private ReferenciaClinica referencia(FichaParaLista ficha,ContextoUsuario contexto){return new ReferenciaClinica("lista_substituicao",ficha.vinculoId(),contexto.exigirNutricionista(),ficha.pacienteId(),contexto.organizacaoId());}
}
