package com.br.dietamais.comum;

import static org.assertj.core.api.Assertions.assertThat;

import com.br.dietamais.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Base para cenários HTTP com dois donos e banco real, sem confiar em IDs do cliente. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties="dieta.auth.login-limite-ip=10000")
@Import(TestcontainersConfiguration.class)
public abstract class TesteIsolamento {
	protected static final String SENHA="Frase fictícia com luas azuis em 2026";
	@Autowired protected JdbcTemplate jdbc;
	@Autowired protected JsonMapper mapper;
	@LocalServerPort int porta;
	private final HttpClient cliente=HttpClient.newHttpClient();
	@AfterEach void fecharCliente(){cliente.close();}
	protected ContaTeste cadastrarNutri() throws Exception {
		String email=UUID.randomUUID()+"@example.invalid";
		var resposta=requisitar("POST","/auth/cadastro-nutri",null,Map.of("nome","Nutri fictícia","email",email,
			"senha",SENHA,"crn","CRN-1 12345","aceitouTermos",true,"versaoTermos","2026-10"));
		assertThat(resposta.statusCode()).isEqualTo(201);
		UUID uid=jdbc.queryForObject("select id from usuario where email=?",UUID.class,email);
		UUID nid=jdbc.queryForObject("select id from nutricionista where usuario_id=?",UUID.class,uid);
		UUID org=jdbc.queryForObject("select organizacao_id from nutricionista where id=?",UUID.class,nid);
		return new ContaTeste(uid,nid,org,email,entrar(email,null));
	}
	protected String entrar(String email,String perfil) throws Exception {
		var corpo=new HashMap<String,Object>();
		corpo.put("email",email);corpo.put("senha",SENHA);
		corpo.put("dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Aparelho fictício"));
		if(perfil!=null)corpo.put("perfil",perfil);
		var resposta=requisitar("POST","/auth/login",null,corpo);
		assertThat(resposta.statusCode()).isEqualTo(200);
		return json(resposta).path("accessToken").asString();
	}
	protected HttpResponse<String> requisitar(String metodo,String caminho,String token,Object corpo) throws Exception {
		var pedido=HttpRequest.newBuilder(URI.create("http://localhost:"+porta+"/api/v1"+caminho));
		if(token!=null)pedido.header("Authorization","Bearer "+token);
		if(corpo!=null)pedido.header("Content-Type","application/json");
		pedido.method(metodo,corpo==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)));
		return cliente.send(pedido.build(),HttpResponse.BodyHandlers.ofString());
	}
	protected JsonNode json(HttpResponse<String> resposta){return mapper.readTree(resposta.body());}
	protected void erro(HttpResponse<String> resposta,int status,String codigo) {
		assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(status);
		assertThat(json(resposta).path("codigo").asString()).isEqualTo(codigo);
		assertThat(resposta.body()).doesNotContain(SENHA,"senha_hash");
	}
	protected record ContaTeste(UUID usuario,UUID nutri,UUID organizacao,String email,String token) { }
}
