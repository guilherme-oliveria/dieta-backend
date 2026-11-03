package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.api.TokenCalendarioResposta;
import com.br.dietamais.agenda.dominio.GeradorIcs;
import com.br.dietamais.agenda.api.*;
import com.br.dietamais.agenda.infra.CalendarioStore;
import com.br.dietamais.agenda.infra.ConsultaStore;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.ConsultaDietaEmUso;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN-DIE-08: o feed reflete sempre a dieta em uso (calculado na leitura, nada a invalidar). RN-ASS-05: assinatura vencida da nutri não corta o paciente. */
@Service
public class GerenciarCalendario {
	private static final SecureRandom ALEATORIO=new SecureRandom();
	private final CalendarioStore store;private final ConsultaStore consultas;private final ConsultaDietaEmUso dietas;private final UsuarioAtual usuario;private final Clock relogio;private final String base;
	public GerenciarCalendario(CalendarioStore store,ConsultaStore consultas,ConsultaDietaEmUso dietas,UsuarioAtual usuario,Clock relogio,@Value("${dieta.publico.url-base:https://api.dietamais.com.br}") String base){
		this.store=store;this.consultas=consultas;this.dietas=dietas;this.usuario=usuario;this.relogio=relogio;this.base=base.endsWith("/")?base.substring(0,base.length()-1):base;
	}
	@Transactional
	public TokenCalendarioResposta gerarToken(){
		var paciente=usuario.obter().exigirPaciente();
		var bytes=new byte[32];ALEATORIO.nextBytes(bytes);var token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		store.gravar(paciente,hash(token),relogio.instant());
		var url=base+"/cal/"+token+".ics";
		return new TokenCalendarioResposta(token,url,url.replaceFirst("^https?://","webcal://"));
	}
	@Transactional
	public void revogar(){store.revogar(usuario.obter().exigirPaciente());}
	/** Texto .ics do token, ou vazio se o token não existe/foi revogado. Leva também as consultas dos últimos 30 dias em diante (canceladas saem como CANCELLED). */
	@Transactional(readOnly=true)
	public Optional<String> feed(String token){
		return store.porHash(hash(token)).map(dono->{
			var fuso=ZoneId.of(dono.fuso());var agora=relogio.instant();
			var eventos=consultas.doPaciente(dono.pacienteId(),agora.minus(Duration.ofDays(30)),agora.plus(Duration.ofDays(400))).stream().map(GerenciarCalendario::evento).toList();
			return dietas.snapshotEmUso(dono.pacienteId()).map(s->GeradorIcs.gerar(s.refeicoes().stream().map(r->new GeradorIcs.Refeicao(r.id(),r.nome(),r.horario())).toList(),eventos,s.publicadoEm(),fuso))
				.orElseGet(()->GeradorIcs.gerar(List.of(),eventos,agora,fuso));
		});
	}
	/** Título genérico: o tipo da consulta não é dado clínico, mas o feed sai do app e vai para calendários de terceiros (RN-NOT-02). */
	private static GeradorIcs.Consulta evento(ConsultaResposta c){
		var titulo=switch(c.tipo()){case PRIMEIRA->"Dieta+: Primeira consulta";case RETORNO->"Dieta+: Consulta de retorno";case ONLINE->"Dieta+: Consulta online";};
		return new GeradorIcs.Consulta(c.id(),c.inicio(),c.inicio().plus(Duration.ofMinutes(c.duracaoMin())),titulo,c.lugar(),c.status()==StatusConsulta.CANCELADA,c.versao()-1,c.atualizadoEm());
	}
	static String hash(String token){
		try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
		catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
	}
}
