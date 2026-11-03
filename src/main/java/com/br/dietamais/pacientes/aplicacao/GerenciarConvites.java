package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.pacientes.dominio.CodigoConvite;
import com.br.dietamais.pacientes.dominio.TermoPublicado;
import com.br.dietamais.pacientes.infra.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GerenciarConvites {
	private final ConviteStore convites;private final FichaPacienteStore fichas;private final UsuarioAtual usuario;
	private final ReservaVagaPaciente vagas;private final GuardaEscritaNutri guarda;private final ConsultaNutricionistas nutris;
	private final ContasPaciente contas;private final EnfileirarEmail emails;private final AuditoriaClinica auditoria;
	private final ApplicationEventPublisher eventos;private final GeradorIds ids;private final Clock relogio;
	private final Duration validade;private final String url;private final String versaoTermo;private final String textoTermo;
	private final CodigoConvite codigos=new CodigoConvite();
	public GerenciarConvites(ConviteStore convites,FichaPacienteStore fichas,UsuarioAtual usuario,ReservaVagaPaciente vagas,
			GuardaEscritaNutri guarda,ConsultaNutricionistas nutris,ContasPaciente contas,EnfileirarEmail emails,AuditoriaClinica auditoria,
			ApplicationEventPublisher eventos,GeradorIds ids,Clock relogio,
			@Value("${dieta.convite.validade:P7D}") Duration validade,@Value("${EMAIL_URL_CONVITE:dietamais://convites}") String url,
			@Value("${dieta.consentimento.versao:}") String versao,@Value("${dieta.consentimento.texto:}") String texto) {
		this.convites=convites;this.fichas=fichas;this.usuario=usuario;this.vagas=vagas;this.guarda=guarda;this.nutris=nutris;
		this.contas=contas;this.emails=emails;this.auditoria=auditoria;this.eventos=eventos;this.ids=ids;this.relogio=relogio;
		if(validade.isZero()||validade.isNegative())throw new IllegalArgumentException("Configuração de convite inválida");
		var endereco=URI.create(url);
		if(!Set.of("https","dietamais").contains(endereco.getScheme())||endereco.getHost()==null||endereco.getRawUserInfo()!=null||endereco.getRawQuery()!=null||endereco.getRawFragment()!=null)throw new IllegalArgumentException("URL de convite inválida");
		this.validade=validade;this.url=url;this.versaoTermo=versao;this.textoTermo=texto;
	}
	@Transactional
	public ConviteCriadoResposta gerar(UUID paciente) {
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();
		if(!contexto.usuario().emailConfirmado())throw new ExcecaoNegocio(CodigoErro.EMAIL_NAO_CONFIRMADO,"RN-CON-05");
		termo();
		if(fichas.consultar(paciente,nutri,false)==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		vagas.validarExistente();guarda.validar();fichas.bloquearEscrita(nutri);
		var ficha=fichas.consultar(paciente,nutri,true);
		if(ficha==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		if(ficha.status()!=StatusVinculo.CONVIDADO)throw CodigoConvite.invalido();
		var agora=relogio.instant();var fim=agora.plus(validade);convites.invalidar(ficha.vinculoId(),agora);
		for(int tentativa=0;tentativa<5;tentativa++) {
			String codigo=codigos.novo();UUID id=ids.novo();
			if(!convites.inserir(id,ficha.vinculoId(),CodigoConvite.hash(codigo),ficha.email(),agora,fim))continue;
			if(ficha.email()!=null)emails.enfileirar(new EmailTransacional(TipoEmail.CONVITE,ficha.email(),codigo,fim));
			auditoria.escrita(new ReferenciaClinica("convite",id,nutri,paciente,contexto.organizacaoId()));
			return new ConviteCriadoResposta(codigo,UriComponentsBuilder.fromUriString(url).pathSegment(codigo).build().encode().toUri(),fim);
		}
		throw new IllegalStateException("Não foi possível gerar código de convite");
	}
	@Transactional(readOnly=true)
	public ConviteResposta consultar(String codigo) {
		var convite=validar(codigo,false);var nutri=nutri(convite.nutri());
		return new ConviteResposta(new NutricionistaResumoResposta(nutri.id(),nutri.nome(),nutri.crn()),termo(),convite.fim());
	}
	@Transactional
	public AceiteConviteResposta aceitar(String codigo,AceitarConviteRequisicao entrada,String ip) {
		var termo=termo();
		if(!Boolean.TRUE.equals(entrada.aceitouTermo())||!termo.versao().equals(entrada.versaoTermo()))throw new ExcecaoNegocio(CodigoErro.CONSENTIMENTO_OBRIGATORIO,"RN-VIN-04");
		var anterior=validar(codigo,false);fichas.bloquearEscrita(anterior.nutri());
		var convite=validar(codigo,true);var ator=usuario.opcional();UUID uid;
		if(ator.isPresent()) {
			if(entrada.cadastro()!=null)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
			uid=ator.get().usuarioId();var conta=contas.bloquear(uid);validarDestinatario(convite,conta.email());
		} else {
			var cadastro=entrada.cadastro();
			if(cadastro==null)throw new ExcecaoNegocio(CodigoErro.NAO_AUTENTICADO,null);
			validarDestinatario(convite,cadastro.email());
			uid=contas.criar(cadastro.nome(),cadastro.email(),cadastro.senha(),cadastro.fuso(),cadastro.aceitouTermos(),cadastro.versaoTermos());
			contas.bloquear(uid);
		}
		UUID paciente=convites.identificarPaciente(convite.paciente(),uid);
		if(convites.duplicado(convite.vinculo(),paciente,convite.nutri()))throw new ExcecaoNegocio(CodigoErro.VINCULO_JA_EXISTE,"RN-VIN-07");
		var agora=relogio.instant();convites.aceitar(convite,paciente,uid,termo.versao(),hashTermo(),ip,agora);
		var sessao=contas.emitir(uid,entrada.dispositivo());var vinculo=resposta(convites.vinculo(convite.vinculo()));
		var nutri=nutri(convite.nutri());
		eventos.publishEvent(new AcessoClinico(uid,Perfil.PACIENTE,new ReferenciaClinica("vinculo",convite.vinculo(),convite.nutri(),paciente,nutri.organizacaoId()),"ESCRITA",agora,ip,MDC.get("traceId")));
		return new AceiteConviteResposta(sessao,vinculo);
	}
	@Transactional
	public VinculoResposta encerrar(UUID paciente,UUID vinculoId) {
		var contexto=usuario.obter();UUID nutri=null;
		if(contexto.perfil()==Perfil.NUTRICIONISTA){nutri=contexto.exigirNutricionista();guarda.validar();}
		else if(contexto.perfil()!=Perfil.PACIENTE)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		else if(!contexto.exigirPaciente().equals(paciente))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		var candidatos=convites.paraEncerrar(paciente,nutri,vinculoId);
		if(candidatos.isEmpty())throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		var selecionado=candidatos.getFirst();
		if(contexto.perfil()==Perfil.PACIENTE&&vinculoId==null&&candidatos.size()>1) {
			var abertos=candidatos.stream().filter(v->v.status()!=StatusVinculo.ENCERRADO).toList();
			if(abertos.size()!=1)throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO,null,"Escolha o vínculo que deseja encerrar.",List.of(new ErroCampoResposta("vinculoId","OBRIGATORIO","Escolha um vínculo.")),null);
			selecionado=abertos.getFirst();
		}
		fichas.bloquearEscrita(selecionado.nutri());selecionado=convites.vinculo(selecionado.id());
		if(!selecionado.paciente().equals(paciente))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		if(selecionado.status()!=StatusVinculo.ENCERRADO)convites.encerrar(selecionado.id(),relogio.instant());
		var dados=nutri(selecionado.nutri());
		auditoria.escrita(new ReferenciaClinica("vinculo",selecionado.id(),selecionado.nutri(),paciente,dados.organizacaoId()));
		return resposta(convites.vinculo(selecionado.id()));
	}
	private ConviteStore.Linha validar(String codigo,boolean bloquear) {
		var convite=convites.consultar(CodigoConvite.hash(codigo),bloquear);
		if(convite==null||!"CONVIDADO".equals(convite.status()))throw CodigoConvite.invalido();
		CodigoConvite.validar(convite.usado(),convite.invalidado(),convite.fim(),relogio.instant());return convite;
	}
	private void validarDestinatario(ConviteStore.Linha convite,String email) {
		if(convite.email()!=null&&!convite.email().equalsIgnoreCase(email.strip()))throw CodigoConvite.invalido();
	}
	private ConsultaNutricionistas.DadosNutri nutri(UUID id){return Objects.requireNonNull(nutris.porIds(Set.of(id)).get(id));}
	private VinculoResposta resposta(ConviteStore.VinculoDados v) {
		var nutri=nutri(v.nutri());return new VinculoResposta(v.id(),v.paciente(),new NutricionistaResumoResposta(nutri.id(),nutri.nome(),nutri.crn()),v.status(),v.consentimento(),v.encerrado(),v.versao(),v.atualizado());
	}
	private String hashTermo() {
		try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(termo().texto().getBytes(StandardCharsets.UTF_8)));}
		catch(NoSuchAlgorithmException erro){throw new IllegalStateException("SHA-256 indisponível",erro);}
	}
	private TermoConsentimentoResposta termo(){return TermoPublicado.exigir(versaoTermo,textoTermo);}
}
