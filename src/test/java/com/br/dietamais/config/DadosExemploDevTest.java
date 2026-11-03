package com.br.dietamais.config;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

class DadosExemploDevTest extends TesteComPaciente {
	@Autowired Environment ambiente;
	@Autowired JsonMapper jsonMapper;
	@Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
	@Test void cargaCriaNutriPacientesEDietaPublicadaDeCercaDe2510KcalEEhIdempotente() throws Exception {
		jdbc.update("delete from usuario where email like '%@exemplo.dev'");
		var carga=new DadosExemploDev(ambiente,jdbc,jsonMapper,encoder);
		carga.run(null);carga.run(null);
		assertThat(jdbc.queryForObject("select count(*) from usuario where email like '%@exemplo.dev'",Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select count(*) from ficha_paciente f join nutricionista n on n.id=f.nutricionista_id join usuario u on u.id=n.usuario_id where u.email='nutri@exemplo.dev'",Integer.class)).isEqualTo(3);
		var token=entrarComo("maria@exemplo.dev");
		var dietas=json(requisitar("GET","/eu/dietas",token,null));
		assertThat(dietas.path("itens")).hasSize(1);
		var kcal=dietas.path("itens").get(0).path("totais").path("kcal").decimalValue();
		assertThat(kcal).isBetween(new java.math.BigDecimal("2500"),new java.math.BigDecimal("2520"));
		assertThat(dietas.path("itens").get(0).path("refeicoes")).hasSize(6);
	}
	String entrarComo(String email) throws Exception {
		var corpo=new java.util.HashMap<String,Object>();corpo.put("email",email);corpo.put("senha",DadosExemploDev.SENHA_TESTE);
		corpo.put("dispositivo",java.util.Map.of("id",java.util.UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Teste"));
		corpo.put("perfil","PACIENTE");
		var r=requisitar("POST","/auth/login",null,corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);return json(r).path("accessToken").asString();
	}
}
