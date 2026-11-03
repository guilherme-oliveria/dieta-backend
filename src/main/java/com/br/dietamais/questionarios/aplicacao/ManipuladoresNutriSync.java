package com.br.dietamais.questionarios.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.api.*;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Escrita da nutri pelo {@code POST /sync} (BE-25): mesmas regras dos endpoints REST. */
@Configuration(value="questionariosManipuladoresNutriSync",proxyBeanMethods=false)
class ManipuladoresNutriSync {
	@Bean ManipuladorMutacao modelosSync(GerenciarModelos s,JsonMapper m,Validator v,UsuarioAtual u){
		return ManipuladorEscritaNutri.de("modelos",m,v,u,SalvarModeloRequisicao.class,ManipuladorEscritaNutri.Paciente.NENHUM,(f,r)->s.criar(r).versao(),SalvarModeloRequisicao.class,(id,im,r)->s.atualizar(id,im,r).versao());
	}
}
