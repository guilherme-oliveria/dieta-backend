package com.br.dietamais.contas.dominio;

import com.br.dietamais.comum.api.*;
import java.time.Instant;

public record TokenUsoUnico(FinalidadeToken finalidade, Instant expiraEm, boolean usado, boolean anulado) {
	public void validar(FinalidadeToken esperada, Instant agora) {
		if (finalidade != esperada || usado || anulado || !agora.isBefore(expiraEm))
			throw new ExcecaoNegocio(CodigoErro.TOKEN_INVALIDO, esperada.regra());
	}
}
