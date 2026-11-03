package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.comum.api.ContaPacienteExcluida;
import com.br.dietamais.pacientes.infra.ExclusaoPacienteStore;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** RN-LGPD-03: anonimiza a identidade do paciente e encerra os vínculos; prontuário e fichas das nutris ficam retidos. */
@Component
public class AnonimizarPacienteContaExcluida {
	private final ExclusaoPacienteStore store;private final Clock relogio;
	public AnonimizarPacienteContaExcluida(ExclusaoPacienteStore store,Clock relogio){this.store=store;this.relogio=relogio;}
	@EventListener public void aoExcluirConta(ContaPacienteExcluida evento){
		store.encerrarVinculosAbertos(evento.pacienteId(),relogio.instant());
		store.anonimizar(evento.pacienteId());
	}
}
