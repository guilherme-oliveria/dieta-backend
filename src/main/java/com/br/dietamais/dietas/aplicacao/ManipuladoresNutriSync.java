package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Escrita da nutri pelo {@code POST /sync} (BE-25): mesmas regras dos endpoints REST. */
@Configuration(value="dietasManipuladoresNutriSync",proxyBeanMethods=false)
class ManipuladoresNutriSync {
	@Bean ManipuladorMutacao dietasSync(GerenciarDietas s,JsonMapper m,Validator v,UsuarioAtual u){
		return ManipuladorEscritaNutri.de("dietas",m,v,u,SalvarDietaRequisicao.class,ManipuladorEscritaNutri.Paciente.FORA_DO_DTO,(f,r)->s.criar(f,r).versao(),SalvarDietaRequisicao.class,(id,im,r)->s.atualizar(id,im,r).versao());
	}
}
