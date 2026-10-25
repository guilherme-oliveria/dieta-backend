package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.br.dietamais.comum.TesteIsolamento;
import com.br.dietamais.comum.api.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;

@Import(UsuarioAtualApiTest.RecursoClinicoTeste.class)
class UsuarioAtualApiTest extends TesteIsolamento {
	@Test void euUsaBancoERejeitaContaInativadaMesmoComTokenValido() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var resposta=requisitar("GET","/eu?usuarioId="+b.usuario(),a.token(),null);
		assertThat(resposta.statusCode()).isEqualTo(200);
		var dados=json(resposta);
		assertThat(dados.path("usuario").path("id").asString()).isEqualTo(a.usuario().toString());
		assertThat(dados.path("assinatura").path("organizacaoId").asString()).isEqualTo(a.organizacao().toString());
		assertThat(dados.path("assinatura").path("pacientesOcupados").asInt()).isZero();
		assertThat(resposta.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
		jdbc.update("update usuario set ativo=false where id=?",a.usuario());
		erro(requisitar("GET","/eu",a.token(),null),401,"NAO_AUTENTICADO");
	}
	@Test void perfisMultiplosMantemVinculosIsoladosEAuditamApenasIdentificadores() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		UUID pid=jdbc.queryForObject("insert into paciente(usuario_id,nome) values(?,?) returning id",UUID.class,a.usuario(),"Paciente fictício");
		UUID outro=jdbc.queryForObject("insert into paciente(nome) values(?) returning id",UUID.class,"Outro fictício");
		UUID vinculo=jdbc.queryForObject("insert into vinculo(nutricionista_id,paciente_id,status) values(?,?,'ATIVO') returning id",UUID.class,b.nutri(),pid);
		jdbc.update("insert into vinculo(nutricionista_id,paciente_id) values(?,?)",a.nutri(),outro);
		String paciente=entrar(a.email(),"PACIENTE");
		var resposta=requisitar("GET","/eu?pacienteId="+outro,paciente,null);
		assertThat(resposta.statusCode()).isEqualTo(200);
		var dados=json(resposta);
		assertThat(dados.path("usuario").path("perfis").size()).isEqualTo(2);
		assertThat(dados.path("usuario").path("perfil").asString()).isEqualTo("PACIENTE");
		assertThat(dados.path("assinatura").isNull()).isTrue();
		assertThat(dados.path("vinculos").size()).isEqualTo(1);
		assertThat(dados.path("vinculos").get(0).path("id").asString()).isEqualTo(vinculo.toString());
		var log=jdbc.queryForMap("select * from log_acesso where usuario_id=? and registro_id=?",a.usuario(),vinculo);
		assertThat(log).containsEntry("acao","LEITURA").containsEntry("perfil","PACIENTE")
			.containsEntry("organizacao_id",b.organizacao()).containsEntry("paciente_id",pid);
		assertThat(log.get("origem_ip")).isNotNull();assertThat(log.get("trace_id")).isNotNull();
		assertThat(log.toString()).doesNotContain(a.email(),"Paciente fictício",paciente);
	}
	@Test void recursoDeOutraNutriRetorna404SemAuditoriaOuAlteracao() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		erro(requisitar("GET","/teste-clinico/"+b.nutri(),a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("POST","/teste-clinico/"+b.nutri(),a.token(),Map.of("nutricionistaId",b.nutri())),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where usuario_id=?",Integer.class,a.usuario())).isZero();
		assertThat(requisitar("GET","/teste-clinico/"+a.nutri(),a.token(),null).statusCode()).isEqualTo(200);
		assertThat(requisitar("POST","/teste-clinico/"+a.nutri(),a.token(),Map.of()).statusCode()).isEqualTo(200);
		assertThat(jdbc.queryForList("select acao from log_acesso where usuario_id=?",String.class,a.usuario())).containsExactlyInAnyOrder("LEITURA","ESCRITA");
	}
	@Test void assinaturaBloqueiaEscritaMasMantemLeituraEPerfilPaciente() throws Exception {
		var a=cadastrarNutri();
		jdbc.update("update assinatura set status='CANCELADA' where organizacao_id=?",a.organizacao());
		erro(requisitar("POST","/teste-clinico/"+a.nutri(),a.token(),Map.of()),422,"ASSINATURA_INATIVA");
		assertThat(requisitar("GET","/teste-clinico/"+a.nutri(),a.token(),null).statusCode()).isEqualTo(200);
		jdbc.update("insert into paciente(usuario_id,nome) values(?,?)",a.usuario(),"Paciente fictício");
		String paciente=entrar(a.email(),"PACIENTE");
		assertThat(requisitar("GET","/eu",paciente,null).statusCode()).isEqualTo(200);
		erro(requisitar("GET","/teste-clinico/"+a.nutri(),paciente,null),403,"PERFIL_INVALIDO");
	}
	@Test void alteracaoDaIdentidadeNoBancoInvalidaClaimsAntigos() throws Exception {
		var a=cadastrarNutri();
		jdbc.update("delete from nutricionista where id=?",a.nutri());
		erro(requisitar("GET","/eu",a.token(),null),401,"NAO_AUTENTICADO");
	}

	/** Rotas apenas de teste para provar o mecanismo antes das APIs clínicas da BE-06. */
	@RestController
	@RotaNutri
	@RequestMapping("/api/v1/teste-clinico")
	static class RecursoClinicoTeste {
		private final UsuarioAtual usuario;
		private final AuditoriaClinica auditoria;
		RecursoClinicoTeste(UsuarioAtual usuario,AuditoriaClinica auditoria){this.usuario=usuario;this.auditoria=auditoria;}
		@GetMapping("/{id}") Map<String,String> ler(@PathVariable UUID id){auditoria.leitura(visivel(id));return Map.of("estado","ok");}
		@PostMapping("/{id}") Map<String,String> escrever(@PathVariable UUID id){auditoria.escrita(visivel(id));return Map.of("estado","ok");}
		private ReferenciaClinica visivel(UUID id) {
			var contexto=usuario.obter();
			if(!contexto.exigirNutricionista().equals(id))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
			return new ReferenciaClinica("recurso_teste",id,id,null,contexto.organizacaoId());
		}
	}
}
