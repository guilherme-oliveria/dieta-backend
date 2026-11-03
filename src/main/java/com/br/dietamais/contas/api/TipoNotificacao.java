package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.Perfil;

/** Tipos de push da RN-NOT-01, cada um destinado a um perfil. */
public enum TipoNotificacao {
	NOVA_DIETA(Perfil.PACIENTE),QUESTIONARIO_ENVIADO(Perfil.PACIENTE),CONSULTA_AMANHA(Perfil.PACIENTE),
	QUESTIONARIO_RESPONDIDO(Perfil.NUTRICIONISTA),PACIENTE_SEM_CHECKIN(Perfil.NUTRICIONISTA),ASSINATURA(Perfil.NUTRICIONISTA);
	private final Perfil perfil;
	TipoNotificacao(Perfil perfil){this.perfil=perfil;}
	public Perfil perfil(){return perfil;}
}
