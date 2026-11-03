package com.br.dietamais.admin;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.comum.TesteIsolamento;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties={"dieta.auth.login-limite-ip=10000","dieta.admin.email=Admin-Inicial@Example.invalid","dieta.admin.senha=senha-inicial-de-teste-12"})
@Import(TestcontainersConfiguration.class)
class CriarAdminInicialApiTest extends TesteIsolamento {
	@Test void criaOAdminNaSubidaUmaUnicaVezEEleEntraComASenhaDoAmbiente() throws Exception {
		assertThat(jdbc.queryForObject("select count(*) from usuario where email='admin-inicial@example.invalid' and perfil='ADMIN' and ativo",Integer.class)).isEqualTo(1);
		var corpo=new java.util.HashMap<String,Object>();corpo.put("email","admin-inicial@example.invalid");corpo.put("senha","senha-inicial-de-teste-12");
		corpo.put("dispositivo",java.util.Map.of("id",java.util.UUID.randomUUID(),"plataforma","DESKTOP","nomeAparelho","Navegador fictício"));
		var r=requisitar("POST","/auth/login",null,corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
		String token=json(r).path("accessToken").asString();
		assertThat(requisitar("GET","/admin/planos",token,null).statusCode()).isEqualTo(200);
		assertThat(jdbc.queryForObject("select senha_hash from usuario where email='admin-inicial@example.invalid'",String.class)).doesNotContain("senha-inicial");
	}
}
