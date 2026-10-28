package com.br.dietamais.admin;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdminApiTest extends TesteComPaciente {
	/** Admin de teste: mesma senha das contas fictícias, perfil ADMIN e e-mail confirmado. */
	String admin() throws Exception {
		String email="admin-"+UUID.randomUUID()+"@example.invalid";
		jdbc.update("insert into usuario(email,senha_hash,nome,perfil,ativo,email_confirmado_em) select ?,senha_hash,'Admin fictício','ADMIN',true,now() from usuario where id=?",email,cadastrarNutri().usuario());
		return entrar(email,null);
	}

	@Test void planosAssinaturasECrnSaoAdministradosComLogEMotivoObrigatorio() throws Exception {
		String t=admin();var nutri=cadastrarNutri();String codigo="PLANO_"+UUID.randomUUID().toString().substring(0,6).toUpperCase().replace("-","X");
		// planos
		var planos=json(requisitar("GET","/admin/planos",t,null));
		assertThat(planos.toString()).contains("TESTE","BASICO_20");
		var corpo=new HashMap<String,Object>(Map.of("codigo",codigo,"nome","Plano de teste","precoMensal",59.9,"limitePacientes",50,"motivo","Novo plano"));
		var criado=requisitar("POST","/admin/planos",t,corpo);assertThat(criado.statusCode()).as(criado.body()).isEqualTo(201);
		String planoId=json(criado).path("id").asString();
		erro(requisitar("POST","/admin/planos",t,corpo),409,"VINCULO_JA_EXISTE");
		corpo.put("codigo","minusculo");erro(requisitar("POST","/admin/planos",t,corpo),400,"FORMATO_INVALIDO");
		var semMotivo=new HashMap<String,Object>(Map.of("nome","Outro","precoMensal",10,"limitePacientes",5,"ativo",true));
		erro(requisitar("PUT","/admin/planos/"+planoId,t,semMotivo),400,"OBRIGATORIO");
		semMotivo.put("motivo","Ajuste de preço");
		var atualizado=requisitar("PUT","/admin/planos/"+planoId,t,semMotivo);assertThat(atualizado.statusCode()).as(atualizado.body()).isEqualTo(200);
		assertThat(json(atualizado).path("precoMensal").decimalValue()).isEqualByComparingTo("10");
		String teste=jdbc.queryForObject("select id::text from plano where codigo='TESTE'",String.class);
		erro(requisitar("PUT","/admin/planos/"+teste,t,Map.of("nome","Teste","precoMensal",0,"limitePacientes",5,"duracaoDias",30,"ativo",false,"motivo","x")),400,"FORMATO_INVALIDO");
		erro(requisitar("PUT","/admin/planos/"+UUID.randomUUID(),t,semMotivo),404,"RECURSO_NAO_ENCONTRADO");
		jdbc.update("delete from plano where id=?::uuid",planoId); // não deixa plano pago extra para outros testes

		// assinaturas
		var lista=json(requisitar("GET","/admin/assinaturas?status=TESTE&tamanho=100",t,null));
		assertThat(lista.path("total").asLong()).isPositive();
		String assinatura=jdbc.queryForObject("select id::text from assinatura where organizacao_id=?",String.class,nutri.organizacao());
		var uma=json(requisitar("GET","/admin/assinaturas/"+assinatura,t,null));
		assertThat(uma.path("status").asString()).isEqualTo("TESTE");assertThat(uma.path("pacientesOcupados").asInt()).isZero();
		erro(requisitar("PATCH","/admin/assinaturas/"+assinatura,t,Map.of("motivo","nada")),400,"OBRIGATORIO");
		erro(requisitar("PATCH","/admin/assinaturas/"+assinatura,t,Map.of("planoCodigo","NAO_EXISTE","motivo","x")),400,"FORMATO_INVALIDO");
		var ajuste=requisitar("PATCH","/admin/assinaturas/"+assinatura,t,Map.of("planoCodigo","BASICO_20","status","ATIVA","fimPeriodoEm","2030-01-01T00:00:00Z","motivo","Cortesia combinada"));
		assertThat(ajuste.statusCode()).as(ajuste.body()).isEqualTo(200);
		assertThat(json(ajuste).path("status").asString()).isEqualTo("ATIVA");assertThat(json(ajuste).path("planoCodigo").asString()).isEqualTo("BASICO_20");
		var log=jdbc.queryForMap("select acao,motivo,antes->>'status' as antes,depois->>'status' as depois from log_admin where alvo_id=?::uuid order by ocorrido_em desc limit 1",assinatura);
		assertThat(log).containsEntry("acao","ASSINATURA_AJUSTADA").containsEntry("motivo","Cortesia combinada").containsEntry("antes","TESTE").containsEntry("depois","ATIVA");
		// o log é só inserção
		assertThatThrownBy(()->jdbc.update("update log_admin set motivo='alterado'")).hasMessageContaining("somente inserção");
		assertThatThrownBy(()->jdbc.update("delete from log_admin")).hasMessageContaining("somente inserção");

		// CRN
		var achada=json(requisitar("GET","/admin/nutricionistas?busca="+nutri.email(),t,null));
		assertThat(achada.path("itens")).hasSize(1);assertThat(achada.path("itens").get(0).path("crnVerificado").asBoolean()).isFalse();
		String nid=nutri.nutri().toString();
		erro(requisitar("PUT","/admin/nutricionistas/"+nid+"/crn-verificado",t,Map.of("verificado",true)),400,"OBRIGATORIO");
		var v=requisitar("PUT","/admin/nutricionistas/"+nid+"/crn-verificado",t,Map.of("verificado",true,"motivo","Consulta no CFN em 28/10"));
		assertThat(v.statusCode()).as(v.body()).isEqualTo(200);assertThat(json(v).path("crnVerificado").asBoolean()).isTrue();
		assertThat(jdbc.queryForObject("select crn_verificado from nutricionista where id=?",Boolean.class,nutri.nutri())).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from log_admin where acao='CRN_VERIFICADO' and alvo_id=?",Integer.class,nutri.nutri())).isEqualTo(1);
		assertThat(json(requisitar("GET","/admin/nutricionistas?verificado=true&busca="+nutri.email(),t,null)).path("itens")).hasSize(1);
		erro(requisitar("PUT","/admin/nutricionistas/"+UUID.randomUUID()+"/crn-verificado",t,Map.of("verificado",true,"motivo","x")),404,"RECURSO_NAO_ENCONTRADO");
	}

	@Test void rotasDeAdminSoParaAdminEAdminNuncaVeDadoClinico() throws Exception {
		String t=admin();var nutri=cadastrarNutri();var p=pacienteVinculado(nutri);var ali=alimentos(nutri.token(),1);
		var dieta=publicar(nutri.token(),criarDieta(nutri.token(),p.fichaId(),dieta("Plano sigiloso",refeicao("Café",0,item(ali.get(0),"1")))));
		String dietaId=dieta.path("dietaId").asString();
		var corpoAv=new HashMap<String,Object>();corpoAv.put("dataHora",java.time.Instant.now().minusSeconds(60).toString());corpoAv.put("pesoKg",70);corpoAv.put("estaturaM",1.75);
		String avaliacao=json(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",nutri.token(),corpoAv)).path("id").asString();

		// nutri e paciente não entram nas rotas de admin
		for(String caminho:List.of("/admin/planos","/admin/assinaturas","/admin/nutricionistas")){
			erro(requisitar("GET",caminho,nutri.token(),null),403,"PERFIL_INVALIDO");
			erro(requisitar("GET",caminho,p.token(),null),403,"PERFIL_INVALIDO");
			assertThat(requisitar("GET",caminho,null,null).statusCode()).isEqualTo(401);
		}
		// RN-LGPD-05: o admin leva 403 em toda rota de dado clínico ou de usuário
		for(String caminho:List.of("/pacientes","/pacientes/"+p.fichaId(),"/pacientes/"+p.fichaId()+"/dietas","/pacientes/"+p.fichaId()+"/avaliacoes","/dietas/"+dietaId,
				"/dietas/"+dietaId+"/pdf","/avaliacoes/"+avaliacao,"/avaliacoes/"+avaliacao+"/pdf","/alimentos","/sync","/eu/dietas","/eu/avaliacoes","/eu/exportacao","/eu/nutri","/assinatura","/eu/avisos"))
			erro(requisitar("GET",caminho,t,null),403,"PERFIL_INVALIDO");
		erro(requisitar("POST","/pacientes",t,Map.of("nome","X")),403,"PERFIL_INVALIDO");
		erro(requisitar("POST","/sync",t,Map.of()),403,"PERFIL_INVALIDO");
		// e as respostas do admin não carregam nada do paciente
		var todas=requisitar("GET","/admin/assinaturas?tamanho=100",t,null).body()+requisitar("GET","/admin/nutricionistas?tamanho=100",t,null).body();
		assertThat(todas).doesNotContain("Paciente fictício",p.email(),"Plano sigiloso");
		// a própria identidade continua acessível
		assertThat(requisitar("GET","/eu",t,null).statusCode()).isEqualTo(200);
	}
}
