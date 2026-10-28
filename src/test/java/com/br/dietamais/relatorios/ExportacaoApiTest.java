package com.br.dietamais.relatorios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.dietas.TesteComPaciente;
import com.br.dietamais.notificacoes.aplicacao.ProcessarEmails;
import com.br.dietamais.relatorios.aplicacao.ProcessadorExportacao;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExportacaoApiTest extends TesteComPaciente {
	@Autowired ProcessadorExportacao processador;
	@Autowired ProcessarEmails emails;
	@Autowired TestcontainersConfiguration.TransporteEmailTeste transporte;

	HttpResponse<byte[]> obter(String urlOuCaminho,boolean seguir) throws Exception {
		var u=URI.create(urlOuCaminho);
		var destino=u.isAbsolute()?"http://localhost:"+porta+u.getRawPath()+(u.getRawQuery()==null?"":"?"+u.getRawQuery()):"http://localhost:"+porta+urlOuCaminho;
		try(var c=HttpClient.newBuilder().followRedirects(seguir?HttpClient.Redirect.ALWAYS:HttpClient.Redirect.NEVER).build()){
			return c.send(HttpRequest.newBuilder(URI.create(destino)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
		}
	}
	Map<String,byte[]> abrir(byte[] zip) throws Exception {
		var entradas=new LinkedHashMap<String,byte[]>();
		try(var z=new ZipInputStream(new ByteArrayInputStream(zip))){for(var e=z.getNextEntry();e!=null;e=z.getNextEntry())entradas.put(e.getName(),z.readAllBytes());}
		return entradas;
	}

	@Test void exportaDadosDoProprioPacienteEmZipComJsonEPdfsEAvisaPorEmail() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var outro=pacienteVinculado(a);var ali=alimentos(a.token(),1);
		publicar(a.token(),criarDieta(a.token(),p.fichaId(),dieta("Plano exportado",refeicao("Café",0,item(ali.get(0),"1")))));
		publicar(a.token(),criarDieta(a.token(),outro.fichaId(),dieta("Plano do outro",refeicao("Café",0,item(ali.get(0),"1")))));
		var corpo=new HashMap<String,Object>();corpo.put("dataHora",Instant.now().minusSeconds(60).toString());corpo.put("pesoKg",70);corpo.put("estaturaM",1.75);
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo).statusCode()).isEqualTo(201);

		erro(requisitar("POST","/eu/exportacao",a.token(),null),403,"PERFIL_INVALIDO");
		assertThat(requisitar("POST","/eu/exportacao",null,null).statusCode()).isEqualTo(401);
		erro(requisitar("GET","/eu/exportacao",p.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		var pedido=requisitar("POST","/eu/exportacao",p.token(),null);assertThat(pedido.statusCode()).as(pedido.body()).isEqualTo(202);
		assertThat(json(pedido).path("status").asString()).isEqualTo("PENDENTE");
		erro(requisitar("POST","/eu/exportacao",p.token(),null),409,"EXPORTACAO_EM_ANDAMENTO");
		assertThat(json(requisitar("GET","/eu/exportacao",p.token(),null)).path("url").isNull()).isTrue();

		processador.processar();
		var pronta=json(requisitar("GET","/eu/exportacao",p.token(),null));
		assertThat(pronta.path("status").asString()).isEqualTo("PRONTA");
		var zip=obter(pronta.path("url").asString(),false);assertThat(zip.statusCode()).isEqualTo(200);
		var arquivos=abrir(zip.body());
		assertThat(arquivos.keySet()).contains("dados.json","LEIAME.txt");
		assertThat(arquivos.keySet().stream().filter(n->n.startsWith("dietas/")&&n.endsWith(".pdf"))).hasSize(1);
		assertThat(arquivos.keySet().stream().filter(n->n.startsWith("avaliacoes/")&&n.endsWith(".pdf"))).hasSize(1);
		var dados=new String(arquivos.get("dados.json"),java.nio.charset.StandardCharsets.UTF_8);
		assertThat(dados).contains("Plano exportado","Paciente fictício").doesNotContain("Plano do outro").doesNotContain("senha");

		// e-mail com o link público; o token só vale para o arquivo do titular
		for(int i=0;i<2000&&emails.processarProxima();i++) { }
		var mensagem=transporte.mensagens.stream().filter(m->m.destinatario().equals(p.email())&&m.assunto().startsWith("Seus dados")).findFirst().orElseThrow();
		var m=Pattern.compile("href=\"([^\"]*/api/v1/exportacoes/[^\"]+)\"").matcher(mensagem.html());assertThat(m.find()).isTrue();
		var redirecionamento=obter(m.group(1),false);assertThat(redirecionamento.statusCode()).isEqualTo(302);
		assertThat(obter(redirecionamento.headers().firstValue("Location").orElseThrow(),false).statusCode()).isEqualTo(200);
		assertThat(obter("/api/v1/exportacoes/token-invalido",false).statusCode()).isEqualTo(404);

		// o outro paciente não vê nem baixa nada; pedido repetido em menos de 1 h segue bloqueado
		erro(requisitar("GET","/eu/exportacao",outro.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("POST","/eu/exportacao",p.token(),null),409,"EXPORTACAO_EM_ANDAMENTO");

		// vencido: arquivo apagado, link morre, novo pedido liberado
		jdbc.update("update exportacao_dados set expira_em=now()-interval '1 minute',criado_em=now()-interval '2 hours' where usuario_id=?::uuid",p.usuarioId());
		processador.processar();
		var expirada=json(requisitar("GET","/eu/exportacao",p.token(),null));
		assertThat(expirada.path("status").asString()).isEqualTo("EXPIRADA");assertThat(expirada.path("url").isNull()).isTrue();
		assertThat(obter(m.group(1),false).statusCode()).isEqualTo(404);
		assertThat(requisitar("POST","/eu/exportacao",p.token(),null).statusCode()).isEqualTo(202);
	}
}
