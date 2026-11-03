package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import java.time.Instant;
import java.util.List;

public class LoginBloqueado extends ExcecaoNegocio {
	private final Instant liberarEm;
	public LoginBloqueado(Instant liberarEm) {
		super(CodigoErro.LOGIN_BLOQUEADO,"RN-CON-03","Aguarde antes de tentar novamente.",List.of(),null);
		this.liberarEm=liberarEm;
	}
	public Instant liberarEm() { return liberarEm; }
}
