package com.br.dietamais.contas.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.br.dietamais.comum.api.CodigoErro;
import com.br.dietamais.comum.api.ErroCampoResposta;
import com.br.dietamais.comum.api.ExcecaoNegocio;

public class PoliticaSenha {
	private final Set<String> bloqueadas;

	public PoliticaSenha(Set<String> bloqueadas) { this.bloqueadas = Set.copyOf(bloqueadas); }

	public void validar(String senha) {
		// RN-CON-02: a comparação normalizada não altera a senha usada no Argon2id.
		if (senha == null || senha.length() < 10 || bloqueadas.contains(hash(senha.strip().toLowerCase(Locale.ROOT)))) {
			throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO, "RN-CON-02", "Escolha uma senha longa que não seja comum.",
					List.of(new ErroCampoResposta("senha", "FORMATO_INVALIDO", "Use ao menos 10 caracteres e evite senhas comuns.")), null);
		}
	}

	private String hash(String valor) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
		catch (NoSuchAlgorithmException erro) { throw new IllegalStateException("SHA-256 indisponível", erro); }
	}
}
