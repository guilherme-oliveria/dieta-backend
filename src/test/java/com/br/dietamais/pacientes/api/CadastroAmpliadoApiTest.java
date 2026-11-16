package com.br.dietamais.pacientes.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.util.*;
import org.junit.jupiter.api.Test;

/** BE-64: cadastro ampliado da ficha (escolaridade, estado civil, profissão, endereço, convênio, indicação, motivo da consulta). */
class CadastroAmpliadoApiTest extends TesteIsolamento {
	static Map<String,Object> cadastro(){
		var m=new LinkedHashMap<String,Object>();m.put("escolaridade","Superior completo");m.put("estadoCivil","Casada");m.put("profissao","Professora");
		m.put("endereco",Map.of("logradouro","Rua Fictícia","numero","10","bairro","Centro","cidade","Cidade Fictícia","uf","sp","cep","01001-000"));
		m.put("convenio","Convênio Fictício");m.put("indicacao","Indicação fictícia");m.put("motivoConsulta","Emagrecimento");return m;
	}
	Map<String,String> ifMatch(long v){return Map.of("If-Match","\""+v+"\"");}

	@Test void criaLeEditaEPreservaOCadastroQuandoOClienteNaoOEnvia() throws Exception {
		var a=cadastrarNutri();
		var corpo=new HashMap<String,Object>(Map.of("nome","Paciente fictício"));corpo.put("cadastroAmpliado",cadastro());
		var criada=requisitar("POST","/pacientes",a.token(),corpo);assertThat(criada.statusCode()).as(criada.body()).isEqualTo(201);
		var p=json(criada);String id=p.path("id").asString();var c=p.path("cadastroAmpliado");
		assertThat(c.path("escolaridade").asString()).isEqualTo("Superior completo");assertThat(c.path("motivoConsulta").asString()).isEqualTo("Emagrecimento");
		assertThat(c.path("endereco").path("uf").asString()).isEqualTo("SP");assertThat(c.path("endereco").path("cep").asString()).isEqualTo("01001000");
		assertThat(json(requisitar("GET","/pacientes/"+id,a.token(),null)).path("cadastroAmpliado").path("profissao").asString()).isEqualTo("Professora");
		// cliente antigo edita sem o campo: o cadastro fica
		var semCampo=requisitar("PATCH","/pacientes/"+id,a.token(),Map.of("nome","Nome novo"),ifMatch(0));assertThat(semCampo.statusCode()).as(semCampo.body()).isEqualTo(200);
		assertThat(json(semCampo).path("cadastroAmpliado").path("convenio").asString()).isEqualTo("Convênio Fictício");
		// com o campo substitui; objeto vazio limpa e some da resposta
		var novo=new HashMap<String,Object>(Map.of("nome","Nome novo"));novo.put("cadastroAmpliado",Map.of("profissao","Médica"));
		var trocado=requisitar("PATCH","/pacientes/"+id,a.token(),novo,ifMatch(1));assertThat(trocado.statusCode()).as(trocado.body()).isEqualTo(200);
		assertThat(json(trocado).path("cadastroAmpliado").path("profissao").asString()).isEqualTo("Médica");assertThat(json(trocado).path("cadastroAmpliado").has("convenio")).isFalse();
		var limpo=new HashMap<String,Object>(Map.of("nome","Nome novo"));limpo.put("cadastroAmpliado",Map.of());
		var apagado=requisitar("PATCH","/pacientes/"+id,a.token(),limpo,ifMatch(2));assertThat(apagado.statusCode()).as(apagado.body()).isEqualTo(200);
		assertThat(json(apagado).has("cadastroAmpliado")).isFalse();
		// vai no sync de pacientes
		var sync=requisitar("GET","/sync?limite=1000",a.token(),null);assertThat(sync.statusCode()).as(sync.body()).isEqualTo(200);assertThat(sync.body()).contains("Nome novo");
	}
	@Test void validaCepUfETamanhosEOutraNutriNaoVe() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var ruim=new HashMap<String,Object>(Map.of("nome","P"));ruim.put("cadastroAmpliado",Map.of("endereco",Map.of("cep","123","uf","SPP")));
		erro(requisitar("POST","/pacientes",a.token(),ruim),400,"FORMATO_INVALIDO");
		var longo=new HashMap<String,Object>(Map.of("nome","P"));longo.put("cadastroAmpliado",Map.of("motivoConsulta","x".repeat(2001)));
		erro(requisitar("POST","/pacientes",a.token(),longo),400,"FORMATO_INVALIDO");
		var corpo=new HashMap<String,Object>(Map.of("nome","P"));corpo.put("cadastroAmpliado",cadastro());
		String id=json(requisitar("POST","/pacientes",a.token(),corpo)).path("id").asString();
		erro(requisitar("GET","/pacientes/"+id,b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
	}
}
