package com.br.dietamais.notificacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.dietas.TesteComPaciente;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.notificacoes.aplicacao.ArquivarAvisos;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AvisosApiTest extends TesteComPaciente {
	@Autowired RegistrarAviso registrar;@Autowired ArquivarAvisos arquivar;

	void novaDieta(ContaTeste a,PacienteTeste p) throws Exception {
		publicar(a.token(),criarDieta(a.token(),p.fichaId(),dieta("Aviso",refeicao("Café",0,item(alimentos(a.token(),1).get(0),"1")))));
	}
	void avisar(PacienteTeste p,int n){
		for(int i=0;i<n;i++)registrar.registrar(new NovoAviso(Perfil.PACIENTE,UUID.fromString(p.fichaId()),TipoAviso.QUESTIONARIO_ENVIADO,null,null)); // sem rota e sem chave
	}

	@Test void centralListaMarcaLidoEIsolaPorUsuarioEPerfil() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var outro=pacienteVinculado(a);
		novaDieta(a,p);avisar(p,2);
		var lista=json(requisitar("GET","/eu/avisos",p.token(),null));
		assertThat(lista.path("itens")).hasSize(3);assertThat(lista.path("total").asLong()).isEqualTo(3);assertThat(lista.path("naoLidos").asLong()).isEqualTo(3);
		var dieta=new ArrayList<tools.jackson.databind.JsonNode>();lista.path("itens").forEach(dieta::add);
		var d=dieta.stream().filter(x->x.path("tipo").asString().equals("NOVA_DIETA")).findFirst().orElseThrow();
		assertThat(d.path("rota").asString()).isEqualTo("/dieta");assertThat(d.path("lidoEm").isNull()).isTrue();assertThat(d.path("versao").asLong()).isEqualTo(1);
		assertThat(json(requisitar("GET","/eu/avisos?pagina=1&tamanho=2",p.token(),null)).path("itens")).hasSize(1);
		// marcar lido: idempotente, soma só uma versão
		String id=d.path("id").asString();
		var lido=json(requisitar("POST","/eu/avisos/"+id+"/lido",p.token(),null));
		assertThat(lido.path("lidoEm").isNull()).isFalse();assertThat(lido.path("versao").asLong()).isEqualTo(2);
		assertThat(json(requisitar("POST","/eu/avisos/"+id+"/lido",p.token(),null)).path("versao").asLong()).isEqualTo(2);
		var naoLidos=json(requisitar("GET","/eu/avisos?somenteNaoLidos=true",p.token(),null));
		assertThat(naoLidos.path("itens")).hasSize(2);assertThat(naoLidos.path("naoLidos").asLong()).isEqualTo(2);
		// isolamento: outro paciente e a nutri não veem nem marcam
		erro(requisitar("POST","/eu/avisos/"+id+"/lido",outro.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("POST","/eu/avisos/"+id+"/lido",a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(json(requisitar("GET","/eu/avisos",outro.token(),null)).path("itens")).isEmpty();
		assertThat(json(requisitar("GET","/eu/avisos",a.token(),null)).path("itens")).isEmpty();
		assertThat(requisitar("GET","/eu/avisos",null,null).statusCode()).isEqualTo(401);
		assertThat(requisitar("GET","/eu/avisos?tamanho=0",p.token(),null).statusCode()).isEqualTo(400);
		// marcar todos
		assertThat(requisitar("POST","/eu/avisos/lidos",p.token(),null).statusCode()).isEqualTo(204);
		assertThat(json(requisitar("GET","/eu/avisos",p.token(),null)).path("naoLidos").asLong()).isZero();
		assertThat(json(requisitar("GET","/eu/avisos",outro.token(),null)).path("total").asLong()).isZero();
	}

	@Test void syncEntregaAvisosELeituraEOArquivamentoViraTombstone() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		avisar(p,1);
		var primeiro=json(requisitar("GET","/sync?limite=1000",p.token(),null));
		var avisos=primeiro.path("mudancas").path("avisos");assertThat(avisos).hasSize(1);
		String id=avisos.get(0).path("id").asString();assertThat(avisos.get(0).path("excluido").asBoolean()).isFalse();
		assertThat(avisos.get(0).path("dados").path("lidoEm").isNull()).isTrue();
		assertThat(json(requisitar("GET","/sync?limite=1000",a.token(),null)).path("mudancas").path("avisos").size()).isZero(); // a nutri não recebe o aviso do paciente
		String cursor=primeiro.path("cursor").asString();
		requisitar("POST","/eu/avisos/"+id+"/lido",p.token(),null);
		var lido=json(requisitar("GET","/sync?limite=1000&cursor="+cursor,p.token(),null));
		assertThat(lido.path("mudancas").path("avisos")).hasSize(1);assertThat(lido.path("mudancas").path("avisos").get(0).path("dados").path("lidoEm").isNull()).isFalse();
		// estado do push não gera mudança de sync
		String cursor2=lido.path("cursor").asString();
		jdbc.update("update aviso set push_status='ENVIADO',push_enviado_em=now() where id=?::uuid",id);
		assertThat(json(requisitar("GET","/sync?limite=1000&cursor="+cursor2,p.token(),null)).path("mudancas").path("avisos").size()).isZero();
		// mais de 90 dias: arquivado, some da central e vira exclusão no sync
		jdbc.update("update aviso set criado_em=now()-interval '91 days' where id=?::uuid",id);
		assertThat(arquivar.executar()).isGreaterThanOrEqualTo(1);
		assertThat(json(requisitar("GET","/eu/avisos",p.token(),null)).path("itens")).isEmpty();
		var apos=json(requisitar("GET","/sync?limite=1000&cursor="+cursor2,p.token(),null)).path("mudancas").path("avisos");
		assertThat(apos).hasSize(1);assertThat(apos.get(0).path("excluido").asBoolean()).isTrue();
	}
}
