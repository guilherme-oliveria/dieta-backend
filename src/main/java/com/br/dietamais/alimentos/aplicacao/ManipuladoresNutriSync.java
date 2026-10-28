package com.br.dietamais.alimentos.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.alimentos.api.*;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Escrita da nutri pelo {@code POST /sync} (BE-25): mesmas regras dos endpoints REST. */
@Configuration(value="alimentosManipuladoresNutriSync",proxyBeanMethods=false)
class ManipuladoresNutriSync {
	@Bean ManipuladorMutacao alimentosSync(GerenciarAlimentos s,JsonMapper m,Validator v,UsuarioAtual u){
		return ManipuladorEscritaNutri.de("alimentos",m,v,u,CriarAlimentoRequisicao.class,ManipuladorEscritaNutri.Paciente.NENHUM,(f,r)->s.criar(r).versao(),AtualizarAlimentoRequisicao.class,(id,im,r)->s.atualizar(id,im,r).versao());
	}
	/** O id da mutação é o da ficha: a lista por paciente nasce com ela. */
	@Bean ManipuladorMutacao listasSync(GerenciarListaPaciente s,JsonMapper m,Validator v,UsuarioAtual u){
		return ManipuladorEscritaNutri.soAtualizacao("listas",m,v,u,AtualizarListaSubstituicaoRequisicao.class,(id,im,r)->s.atualizar(id,im,r).versao());
	}
}
