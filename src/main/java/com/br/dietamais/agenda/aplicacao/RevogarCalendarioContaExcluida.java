package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.infra.CalendarioStore;
import com.br.dietamais.comum.api.ContaPacienteExcluida;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** O feed .ics do paciente morre com a conta (RN-LGPD-03). */
@Component
public class RevogarCalendarioContaExcluida {
	private final CalendarioStore calendario;
	public RevogarCalendarioContaExcluida(CalendarioStore calendario){this.calendario=calendario;}
	@EventListener public void aoExcluirConta(ContaPacienteExcluida evento){calendario.revogar(evento.pacienteId());}
}
