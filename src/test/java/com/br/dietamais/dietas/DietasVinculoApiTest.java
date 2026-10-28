package com.br.dietamais.dietas;

import com.br.dietamais.comum.TesteIsolamento;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DietasVinculoApiTest extends TesteIsolamento {
	@Test void novoVinculoAtivoNaoAutorizaEditarDietaDoHistoricoEncerrado()throws Exception{
		var conta=cadastrarNutri();var cadastro=requisitar("POST","/pacientes",conta.token(),Map.of("nome","Paciente fictício","email",UUID.randomUUID()+"@example.invalid"));assertThat(cadastro.statusCode()).isEqualTo(201);
		var paciente=json(cadastro);String pid=paciente.path("id").asString();String antigo=paciente.path("vinculoId").asString();
		var corpo=Map.of("titulo","Histórico fictício","refeicoes",List.of());var criacao=requisitar("POST","/pacientes/"+pid+"/dietas",conta.token(),corpo);assertThat(criacao.statusCode()).as(criacao.body()).isEqualTo(201);
		var dieta=json(criacao);String id=dieta.path("id").asString();String etag="\""+dieta.path("versao").asLong()+"\"";
		assertThat(requisitar("POST","/pacientes/"+pid+"/encerrar-vinculo",conta.token(),null).statusCode()).isEqualTo(200);
		UUID novo=jdbc.queryForObject("select uuidv7()",UUID.class);UUID pacienteId=UUID.fromString(pid);
		jdbc.update("insert into vinculo(id,nutricionista_id,paciente_id,status) values(?,?,?,'ATIVO')",novo,conta.nutri(),pacienteId);
		jdbc.update("insert into ficha_paciente(id,vinculo_id,nutricionista_id,paciente_id,nome) values(?,?,?,?,?)",novo,novo,conta.nutri(),pacienteId,"Ficha atual fictícia");
		assertThat(jdbc.queryForObject("select status from vinculo where id=?",String.class,UUID.fromString(antigo))).isEqualTo("ENCERRADO");
		assertThat(json(requisitar("GET","/pacientes/"+pid,conta.token(),null)).path("vinculoId").asString()).isEqualTo(novo.toString());
		var tentativa=requisitar("PUT","/dietas/"+id,conta.token(),Map.of("titulo","Alteração indevida fictícia","refeicoes",List.of()),Map.of("If-Match",etag));erro(tentativa,403,"ACESSO_NEGADO");assertThat(json(tentativa).path("regra").asString()).isEqualTo("RN-VIN-05");
		var historico=requisitar("GET","/dietas/"+id,conta.token(),null);assertThat(historico.statusCode()).isEqualTo(200);assertThat(json(historico)).isEqualTo(dieta);
		var atual=requisitar("POST","/pacientes/"+pid+"/dietas",conta.token(),Map.of("titulo","Atual fictícia","refeicoes",List.of()));assertThat(atual.statusCode()).as(atual.body()).isEqualTo(201);var nova=json(atual);assertThat(nova.path("vinculoId").asString()).isEqualTo(novo.toString());
		assertThat(requisitar("PUT","/dietas/"+nova.path("id").asString(),conta.token(),Map.of("titulo","Atual editada fictícia","refeicoes",List.of()),Map.of("If-Match","\""+nova.path("versao").asLong()+"\"")).statusCode()).isEqualTo(200);
	}
}
