package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.comum.api.ContaPacienteExcluida;
import com.br.dietamais.notificacoes.infra.AvisoStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LimparAvisosContaExcluida {
	private final AvisoStore avisos;
	public LimparAvisosContaExcluida(AvisoStore avisos){this.avisos=avisos;}
	@EventListener public void aoExcluirConta(ContaPacienteExcluida evento){avisos.apagarDoPaciente(evento.usuarioId());}
}
