package com.br.dietamais.relatorios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class PdfApiTest extends TesteComPaciente {
	static final byte[] PNG={(byte)0x89,'P','N','G',0x0D,0x0A,0x1A,0x0A,0,0,0,0x0D,'I','H','D','R'};

	HttpResponse<byte[]> baixar(String url) throws Exception {
		var u=URI.create(url);
		try(var c=HttpClient.newHttpClient()){
			return c.send(HttpRequest.newBuilder(URI.create("http://localhost:"+porta+u.getRawPath()+"?"+u.getRawQuery())).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
		}
	}
	HttpResponse<String> enviarLogo(String token,byte[] corpo,String tipo) throws Exception {
		try(var c=HttpClient.newHttpClient()){
			return c.send(HttpRequest.newBuilder(URI.create("http://localhost:"+porta+"/api/v1/eu/nutri/logo")).header("Authorization","Bearer "+token).header("Content-Type",tipo)
				.PUT(HttpRequest.BodyPublishers.ofByteArray(corpo)).build(),HttpResponse.BodyHandlers.ofString());
		}
	}
	String texto(byte[] pdf) throws Exception {
		try(var doc=org.apache.pdfbox.pdmodel.PDDocument.load(pdf)){return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);}
	}

	@Test void pdfDaDietaTemCabecalhoDaNutriEPacienteEoLinkExpira() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var ali=alimentos(a.token(),1);
		var rascunho=criarDieta(a.token(),p.fichaId(),dieta("Plano de teste",refeicao("Café da manhã",0,item(ali.get(0),"2"))));
		erro(requisitar("GET","/dietas/"+rascunho.path("id").asString()+"/pdf",a.token(),null),404,"RECURSO_NAO_ENCONTRADO"); // nunca publicada
		publicar(a.token(),rascunho);
		var r=requisitar("GET","/dietas/"+rascunho.path("id").asString()+"/pdf",a.token(),null);
		assertThat(r.statusCode()).as(r.body()).isEqualTo(200);assertThat(r.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
		var pdf=baixar(json(r).path("url").asString());
		assertThat(pdf.statusCode()).isEqualTo(200);assertThat(new String(pdf.body(),0,5)).isEqualTo("%PDF-");
		assertThat(pdf.headers().firstValue("Content-Disposition").orElse("")).contains("dieta-plano-de-teste.pdf");
		var t=texto(pdf.body());
		assertThat(t).contains("Plano de teste","Café da manhã","Nutri fictícia","CRN-1 12345","Paciente fictício");
		// link adulterado ou vencido não entrega
		var url=json(r).path("url").asString();
		assertThat(baixar(url.replace("sig=","sig=0")).statusCode()).isEqualTo(403);
		// paciente baixa o próprio snapshot; outro paciente e outra nutri recebem 404
		var meu=requisitar("GET","/eu/dietas/"+rascunho.path("id").asString()+"/pdf",p.token(),null);assertThat(meu.statusCode()).as(meu.body()).isEqualTo(200);
		assertThat(new String(baixar(json(meu).path("url").asString()).body(),0,5)).isEqualTo("%PDF-");
		var outro=pacienteVinculado(a);var b=cadastrarNutri();
		erro(requisitar("GET","/eu/dietas/"+rascunho.path("id").asString()+"/pdf",outro.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/dietas/"+rascunho.path("id").asString()+"/pdf",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/dietas/"+rascunho.path("id").asString()+"/pdf",p.token(),null),403,"PERFIL_INVALIDO");
		erro(requisitar("GET","/eu/dietas/"+rascunho.path("id").asString()+"/pdf",a.token(),null),403,"PERFIL_INVALIDO");
		assertThat(requisitar("GET","/dietas/"+rascunho.path("id").asString()+"/pdf",null,null).statusCode()).isEqualTo(401);
		// leitura da nutri fica auditada
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where entidade='dieta' and registro_id=?::uuid and acao='LEITURA'",Integer.class,rascunho.path("id").asString())).isPositive();
	}

	@Test void pdfDaAvaliacaoRespeitaCompartilhamentoELogoEntraNoCabecalho() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var b=cadastrarNutri();
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/avaliacoes",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		var corpo=new HashMap<String,Object>();corpo.put("dataHora",Instant.now().minusSeconds(60).toString());corpo.put("pesoKg",70);corpo.put("estaturaM",1.75);corpo.put("parecer","Parecer fictício");
		var av=json(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo));String id=av.path("id").asString();
		// logo: só PNG/JPEG de verdade
		assertThat(enviarLogo(a.token(),"não sou imagem".getBytes(),"image/png").statusCode()).isEqualTo(400);
		assertThat(enviarLogo(a.token(),new byte[600*1024],"image/png").statusCode()).isEqualTo(400);
		assertThat(enviarLogo(a.token(),PNG,"image/png").statusCode()).isEqualTo(204);
		assertThat(json(requisitar("GET","/eu/nutri",a.token(),null)).path("temLogo").asBoolean()).isTrue();
		var r=requisitar("GET","/avaliacoes/"+id+"/pdf",a.token(),null);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
		var t=texto(baixar(json(r).path("url").asString()).body());
		assertThat(t).contains("Avaliação física","70,0 kg","1,75 m","22,9","Parecer fictício","Paciente fictício");
		// compartilhada: paciente baixa; não compartilhada: 404 (RN-AVA-05)
		var meu=requisitar("GET","/eu/avaliacoes/"+id+"/pdf",p.token(),null);assertThat(meu.statusCode()).as(meu.body()).isEqualTo(200);
		jdbc.update("update avaliacao set compartilhada=false where id=?::uuid",id);
		erro(requisitar("GET","/eu/avaliacoes/"+id+"/pdf",p.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/avaliacoes/"+id+"/pdf",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(requisitar("DELETE","/eu/nutri/logo",a.token(),null).statusCode()).isEqualTo(204);
		assertThat(json(requisitar("GET","/eu/nutri",a.token(),null)).path("temLogo").asBoolean()).isFalse();
		assertThat(requisitar("GET","/avaliacoes/"+id+"/pdf",a.token(),null).statusCode()).isEqualTo(200);
	}
}
