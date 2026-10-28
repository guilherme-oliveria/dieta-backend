package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.avaliacoes.api.*;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Escrita da nutri pelo {@code POST /sync} (BE-25): mesmas regras dos endpoints REST. */
@Configuration(value="avaliacoesManipuladoresNutriSync",proxyBeanMethods=false)
class ManipuladoresNutriSync {
	@Bean ManipuladorMutacao avaliacoesSync(GerenciarAvaliacoes s,JsonMapper m,Validator v,UsuarioAtual u){
		return ManipuladorEscritaNutri.de("avaliacoes",m,v,u,SalvarAvaliacaoRequisicao.class,ManipuladorEscritaNutri.Paciente.FORA_DO_DTO,(f,r)->s.criar(f,r).versao(),SalvarAvaliacaoRequisicao.class,(id,im,r)->s.atualizar(id,im,r).versao());
	}
}
