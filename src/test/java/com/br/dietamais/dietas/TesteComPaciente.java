package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.assertThat;
import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.comum.TesteIsolamento;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;

/** Base para cenários com nutri, ficha, convite aceito (conta de paciente) e dietas. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"dieta.auth.login-limite-ip=10000","dieta.convite.limite-ip=10000",
	"dieta.consentimento.versao=clinico-teste-v1","dieta.consentimento.texto=Termo clínico fictício exclusivamente para teste"})
@Import(TestcontainersConfiguration.class)
public abstract class TesteComPaciente extends TesteIsolamento {
	protected record PacienteTeste(String fichaId,String vinculoId,String pacienteId,String usuarioId,String token,String email) {}
	@Override protected ContaTeste cadastrarNutri() throws Exception {
		var conta=super.cadastrarNutri();jdbc.update("update usuario set email_confirmado_em=now() where id=?",conta.usuario());return conta;
	}
	/** Cria a ficha, convida e aceita o convite; devolve o paciente já autenticado. */
	protected PacienteTeste pacienteVinculado(ContaTeste nutri) throws Exception {
		String email=UUID.randomUUID()+"@example.invalid";
		var ficha=json(requisitar("POST","/pacientes",nutri.token(),Map.of("nome","Paciente fictício","email",email)));
		String codigo=json(requisitar("POST","/pacientes/"+ficha.path("id").asString()+"/convite",nutri.token(),null)).path("codigo").asString();
		var corpo=new HashMap<String,Object>();corpo.put("aceitouTermo",true);corpo.put("versaoTermo","clinico-teste-v1");
		corpo.put("dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Aparelho fictício"));
		corpo.put("cadastro",Map.of("nome","Paciente fictício","email",email,"senha",SENHA,"aceitouTermos",true,"versaoTermos","2026-10"));
		var aceite=requisitar("POST","/convites/"+codigo+"/aceitar",null,corpo);assertThat(aceite.statusCode()).as(aceite.body()).isEqualTo(200);
		var dados=json(aceite);
		return new PacienteTeste(ficha.path("id").asString(),dados.path("vinculo").path("id").asString(),ficha.path("id").asString(),dados.path("sessao").path("usuario").path("id").asString(),
			dados.path("sessao").path("accessToken").asString(),email);
	}
	protected List<JsonNode> alimentos(String token,int quantidade) throws Exception {
		var lista=new ArrayList<JsonNode>();json(requisitar("GET","/alimentos?tamanho="+quantidade,token,null)).path("itens").forEach(lista::add);return lista;
	}
	protected JsonNode criarDieta(String token,String pacienteFicha,Map<String,Object> corpo) throws Exception {
		var r=requisitar("POST","/pacientes/"+pacienteFicha+"/dietas",token,corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r);
	}
	protected JsonNode publicar(String token,JsonNode dieta) throws Exception {
		var r=requisitar("POST","/dietas/"+dieta.path("id").asString()+"/publicar",token,null,Map.of("If-Match","\""+dieta.path("versao").asLong()+"\""));assertThat(r.statusCode()).as(r.body()).isEqualTo(200);return json(r);
	}
	protected Map<String,Object> dieta(String titulo,Object... refeicoes){var m=new HashMap<String,Object>();m.put("titulo",titulo);m.put("refeicoes",new ArrayList<>(List.of(refeicoes)));return m;}
	protected Map<String,Object> refeicao(String nome,int ordem,Object... itens){var m=new HashMap<String,Object>();m.put("nome",nome);m.put("horario","08:00:00");m.put("ordem",ordem);m.put("itens",new ArrayList<>(List.of(itens)));return m;}
	protected Map<String,Object> item(JsonNode alimento,String porcoes){var m=new HashMap<String,Object>();m.put("alimentoId",alimento.path("id").asString());m.put("porcoes",new BigDecimal(porcoes));return m;}
}
