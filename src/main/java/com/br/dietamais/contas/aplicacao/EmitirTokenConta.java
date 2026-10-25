package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.GeradorIds;
import com.br.dietamais.contas.dominio.FinalidadeToken;
import com.br.dietamais.contas.infra.TokenContaStore;
import com.br.dietamais.notificacoes.api.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class EmitirTokenConta {
	private final TokenContaStore tokens;
	private final EnfileirarEmail emails;
	private final GeradorIds ids;
	private final Clock relogio;
	private final SecureRandom aleatorio=new SecureRandom();
	public EmitirTokenConta(TokenContaStore tokens,EnfileirarEmail emails,GeradorIds ids,Clock relogio) {
		this.tokens=tokens;this.emails=emails;this.ids=ids;this.relogio=relogio;
	}
	@Transactional(propagation=Propagation.MANDATORY)
	public void emitir(UUID usuario,String email,FinalidadeToken finalidade) {
		byte[] bytes=new byte[32];aleatorio.nextBytes(bytes);
		String valor=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		var agora=relogio.instant();var fim=agora.plus(finalidade==FinalidadeToken.CONFIRMAR_EMAIL?Duration.ofHours(24):Duration.ofHours(1));
		tokens.anular(usuario,finalidade,agora);
		tokens.guardar(ids.novo(),usuario,finalidade,hash(valor),agora,fim);
		emails.enfileirar(new EmailTransacional(finalidade==FinalidadeToken.CONFIRMAR_EMAIL?TipoEmail.CONFIRMAR_EMAIL:TipoEmail.REDEFINIR_SENHA,email,valor,fim));
	}
	public static String hash(String valor) {
		try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));}
		catch(NoSuchAlgorithmException erro){throw new IllegalStateException("SHA-256 indisponível");}
	}
}
