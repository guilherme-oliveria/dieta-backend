package com.br.dietamais.pacientes.api;

import java.util.UUID;

public interface OcupacaoPlano {
	int daOrganizacaoAtual();
	/** Só a contagem, para quem age sem usuário logado ou como admin (RN-LGPD-05: nenhum dado do paciente sai daqui). */
	int daOrganizacao(UUID organizacaoId);
}
