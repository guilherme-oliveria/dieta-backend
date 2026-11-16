package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.infra.ConsultaStore;
import com.br.dietamais.pacientes.api.AgendaDosPacientes;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BE-66: próxima consulta agendada de cada vínculo, só das consultas da própria nutri. */
@Service
public class ConsultarProximasConsultas implements AgendaDosPacientes {
	private final ConsultaStore store;
	public ConsultarProximasConsultas(ConsultaStore store){this.store=store;}
	@Override @Transactional(readOnly=true)
	public Map<UUID,Instant> proximaConsulta(Collection<UUID> vinculos,UUID nutri,Instant agora){return store.proximas(vinculos,nutri,agora);}
}
