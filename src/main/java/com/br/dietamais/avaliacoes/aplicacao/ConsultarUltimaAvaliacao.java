package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.infra.AvaliacaoStore;
import com.br.dietamais.pacientes.api.UltimaAvaliacaoDosPacientes;
import com.br.dietamais.pacientes.api.UltimaAvaliacaoResumo;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BE-66: avaliação mais recente de cada vínculo, só das avaliações da própria nutri. */
@Service
public class ConsultarUltimaAvaliacao implements UltimaAvaliacaoDosPacientes {
	private final AvaliacaoStore store;
	public ConsultarUltimaAvaliacao(AvaliacaoStore store){this.store=store;}
	@Override @Transactional(readOnly=true)
	public Map<UUID,UltimaAvaliacaoResumo> ultima(Collection<UUID> vinculos,UUID nutri){return store.ultimas(vinculos,nutri);}
}
