package com.br.dietamais.relatorios.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.pacientes.api.ConsultaCabecalhoRelatorio;
import com.br.dietamais.relatorios.api.PdfResposta;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * PDF de dieta e de avaliação (BE-28). Gera na hora, guarda no armazenamento com nome aleatório e devolve um link que vale poucos minutos;
 * o arquivo é apagado pelo {@link LimpezaPdf} logo depois. O acesso à dieta/avaliação é o mesmo das telas (dono ou paciente do vínculo).
 */
@Service
public class ServicoPdf {
	static final Duration VALIDADE=Duration.ofMinutes(10);
	private final ConsultaDietaParaPdf dietas;private final ConsultaAvaliacaoParaPdf avaliacoes;private final ConsultaCabecalhoRelatorio cabecalhos;
	private final ConsultaNutricionistas nutricionistas;private final Armazenamento armazenamento;private final UsuarioAtual usuario;private final AuditoriaClinica auditoria;private final GeradorIds ids;
	private final GeradorPdf gerador=new GeradorPdf();
	public ServicoPdf(ConsultaDietaParaPdf dietas,ConsultaAvaliacaoParaPdf avaliacoes,ConsultaCabecalhoRelatorio cabecalhos,ConsultaNutricionistas nutricionistas,
			Armazenamento armazenamento,UsuarioAtual usuario,AuditoriaClinica auditoria,GeradorIds ids){
		this.dietas=dietas;this.avaliacoes=avaliacoes;this.cabecalhos=cabecalhos;this.nutricionistas=nutricionistas;this.armazenamento=armazenamento;this.usuario=usuario;this.auditoria=auditoria;this.ids=ids;
	}

	public PdfResposta dietaDaNutri(UUID dietaId){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();
		var s=dietas.daNutri(dietaId);
		auditoria.leitura(new ReferenciaClinica("dieta",s.dietaId(),nutri,s.pacienteId(),contexto.organizacaoId()));
		return entregar(gerador.dieta(cabecalhoDieta(s),s),"dieta-"+slug(s.titulo())+".pdf");
	}
	public PdfResposta dietaDoPaciente(UUID dietaId){
		var s=dietas.doPaciente(dietaId);
		return entregar(gerador.dieta(cabecalhoDieta(s),s),"dieta-"+slug(s.titulo())+".pdf");
	}
	public PdfResposta avaliacaoDaNutri(UUID id){
		usuario.obter().exigirNutricionista();
		var a=avaliacoes.daNutri(id);
		return entregar(gerador.avaliacao(cabecalhoAvaliacao(a),a),"avaliacao.pdf");
	}
	public PdfResposta avaliacaoDoPaciente(UUID id){
		usuario.obter().exigirPaciente();
		var a=avaliacoes.doPaciente(id);
		return entregar(gerador.avaliacao(cabecalhoAvaliacao(a),a),"avaliacao.pdf");
	}

	/** PDFs sem checagem de acesso: o chamador já autorizou (exportação do próprio paciente, BE-29). */
	byte[] dietaBytes(DietaSnapshotResposta s){return gerador.dieta(cabecalhoDieta(s),s);}
	byte[] avaliacaoBytes(AvaliacaoResposta a){return gerador.avaliacao(cabecalhoAvaliacao(a),a);}

	private GeradorPdf.Cabecalho cabecalhoDieta(DietaSnapshotResposta s){
		var nome=cabecalhos.doVinculo(s.vinculoId()).map(ConsultaCabecalhoRelatorio.Cabecalho::paciente).orElse(null);
		return new GeradorPdf.Cabecalho(s.nutricionista().nome(),s.nutricionista().crn(),nome,logo(s.nutricionista().id()));
	}
	private GeradorPdf.Cabecalho cabecalhoAvaliacao(AvaliacaoResposta a){
		var cab=cabecalhos.doVinculo(a.vinculoId()).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		var nutri=nutricionistas.porIds(Set.of(cab.nutricionistaId())).get(cab.nutricionistaId());
		if(nutri==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return new GeradorPdf.Cabecalho(nutri.nome(),nutri.crn(),cab.paciente(),logo(nutri.id()));
	}
	private GeradorPdf.Logo logo(UUID nutri){
		return nutricionistas.logoChave(nutri).flatMap(chave->armazenamento.ler(chave).map(b->new GeradorPdf.Logo(b,chave.endsWith(".png")?"image/png":"image/jpeg"))).orElse(null);
	}
	private PdfResposta entregar(byte[] pdf,String nome){
		String chave=LimpezaPdf.PREFIXO+"/"+ids.novo()+".pdf";
		armazenamento.guardar(chave,pdf,"application/pdf");
		var acesso=armazenamento.urlAssinada(chave,VALIDADE,nome,"application/pdf");
		return new PdfResposta(acesso.url(),acesso.expiraEm());
	}
	static String slug(String t){
		String s=Normalizer.normalize(t==null?"":t,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase().replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");
		return s.isEmpty()?"plano":s.substring(0,Math.min(40,s.length()));
	}
}
