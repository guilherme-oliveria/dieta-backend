package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.api.TokenCalendarioResposta;
import com.br.dietamais.agenda.dominio.GeradorIcs;
import com.br.dietamais.agenda.infra.CalendarioStore;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.ConsultaDietaEmUso;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
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
	private final CalendarioStore store;private final ConsultaDietaEmUso dietas;private final UsuarioAtual usuario;private final Clock relogio;private final String base;
	public GerenciarCalendario(CalendarioStore store,ConsultaDietaEmUso dietas,UsuarioAtual usuario,Clock relogio,@Value("${dieta.publico.url-base:https://api.dietamais.com.br}") String base){
		this.store=store;this.dietas=dietas;this.usuario=usuario;this.relogio=relogio;this.base=base.endsWith("/")?base.substring(0,base.length()-1):base;
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
	/** Texto .ics do token, ou vazio se o token não existe/foi revogado. */
	@Transactional(readOnly=true)
	public Optional<String> feed(String token){
		return store.porHash(hash(token)).map(dono->{
			var fuso=ZoneId.of(dono.fuso());
			return dietas.snapshotEmUso(dono.pacienteId()).map(s->GeradorIcs.gerar(s.refeicoes().stream().map(r->new GeradorIcs.Refeicao(r.id(),r.nome(),r.horario())).toList(),s.publicadoEm(),fuso))
				.orElseGet(()->GeradorIcs.gerar(java.util.List.of(),relogio.instant(),fuso));
		});
	}
	static String hash(String token){
		try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
		catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
	}
}
