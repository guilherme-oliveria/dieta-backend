package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Escrita da nutri pelo {@code POST /sync} (BE-25): mesmas regras dos endpoints REST. */
@Configuration(value="pacientesManipuladoresNutriSync",proxyBeanMethods=false)
class ManipuladoresNutriSync {
	@Bean ManipuladorMutacao pacientesSync(GerenciarPacientes s,JsonMapper m,Validator v,UsuarioAtual u){
		return ManipuladorEscritaNutri.de("pacientes",m,v,u,CriarPacienteRequisicao.class,ManipuladorEscritaNutri.Paciente.NENHUM,(f,r)->s.criar(r).versao(),AtualizarPacienteRequisicao.class,(id,im,r)->s.atualizar(id,im,r).versao());
	}
}
