package com.br.dietamais.config;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.comum.TesteIsolamento;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"dieta.auth.login-limite-ip=10000","dieta.taxa.usuario-por-minuto=5",
	"dieta.app.versao-minima=1.2.0","dieta.seguranca.hsts=true","dieta.seguranca.cors-origens=https://site.exemplo.com.br"})
@Import(TestcontainersConfiguration.class)
class EndurecimentoApiTest extends TesteIsolamento {
	HttpResponse<String> enviar(String metodo,String caminho,Map<String,String> cabecalhos) throws Exception {
		try(var c=HttpClient.newHttpClient()){
			var b=HttpRequest.newBuilder(URI.create("http://localhost:"+porta+caminho)).method(metodo,HttpRequest.BodyPublishers.noBody());
			cabecalhos.forEach(b::header);
			return c.send(b.build(),HttpResponse.BodyHandlers.ofString());
		}
	}

	@Test void versaoMinimaDoAppBloqueiaSoQuemInformaVersaoAntiga() throws Exception {
		var login="/api/v1/auth/login";
		var velha=enviar("POST",login,Map.of("X-App-Versao","1.1.9","Content-Type","application/json"));
		erro(velha,426,"VERSAO_APP_OBSOLETA");
		erro(enviar("POST",login,Map.of("X-App-Versao","0.9","Content-Type","application/json")),426,"VERSAO_APP_OBSOLETA");
		for(String ok:List.of("1.2.0","1.10.0","2.0.0-beta.1","v1.3","lixo"))
			assertThat(enviar("POST",login,Map.of("X-App-Versao",ok,"Content-Type","application/json")).statusCode()).as(ok).isNotEqualTo(426);
		assertThat(enviar("POST",login,Map.of("Content-Type","application/json")).statusCode()).isNotEqualTo(426); // sem cabeçalho: site, curl
		assertThat(enviar("GET","/actuator/health/liveness",Map.of("X-App-Versao","0.0.1")).statusCode()).isEqualTo(200); // fora de /api/v1
	}

	@Test void limiteGeralPorUsuarioDevolve429ComRetryAfterSemAfetarOutroUsuario() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		for(int i=0;i<5;i++)assertThat(requisitar("GET","/eu",a.token(),null).statusCode()).isEqualTo(200);
		var excedeu=requisitar("GET","/eu",a.token(),null);
		erro(excedeu,429,"TAXA_EXCEDIDA");
		assertThat(Long.parseLong(excedeu.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L,60L);
		assertThat(requisitar("GET","/eu",b.token(),null).statusCode()).isEqualTo(200);
	}

	@Test void corsSoParaAOrigemConfiguradaECabecalhosDeSeguranca() throws Exception {
		var permitida=enviar("OPTIONS","/api/v1/eu",Map.of("Origin","https://site.exemplo.com.br","Access-Control-Request-Method","GET","Access-Control-Request-Headers","authorization"));
		assertThat(permitida.statusCode()).isEqualTo(200);
		assertThat(permitida.headers().firstValue("Access-Control-Allow-Origin")).contains("https://site.exemplo.com.br");
		assertThat(permitida.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase()).contains("authorization");
		var negada=enviar("OPTIONS","/api/v1/eu",Map.of("Origin","https://malicioso.example","Access-Control-Request-Method","GET"));
		assertThat(negada.statusCode()).isEqualTo(403);assertThat(negada.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();

		var r=enviar("GET","/actuator/health/liveness",Map.of());
		assertThat(r.headers().firstValue("Content-Security-Policy").orElse("")).contains("default-src 'none'","frame-ancestors 'none'");
		assertThat(r.headers().firstValue("X-Frame-Options")).contains("DENY");
		assertThat(r.headers().firstValue("Referrer-Policy")).contains("no-referrer");
		assertThat(r.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
		assertThat(r.headers().firstValue("Strict-Transport-Security").orElse("")).contains("max-age=31536000").contains("includeSubDomains");
	}
}
