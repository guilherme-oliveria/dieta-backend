package com.br.dietamais.rotina.aplicacao;

import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.rotina.infra.SemCheckinStore;
import java.time.Clock;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * RN-NOT-01: avisa a nutri quando um paciente está há 3 dias sem check-in. A chave de dedup inclui a data da última atividade:
 * um aviso por período parado, e um novo só depois de o paciente voltar e parar de novo. O texto não identifica o paciente.
 */
@Service
public class AlertaSemCheckin {
	static final int DIAS=3;
	private final SemCheckinStore store;private final RegistrarAviso avisos;private final Clock relogio;
	public AlertaSemCheckin(SemCheckinStore store,RegistrarAviso avisos,Clock relogio){this.store=store;this.avisos=avisos;this.relogio=relogio;}

	@Scheduled(fixedDelayString="${dieta.avisos.intervalo-sem-checkin:PT1H}",initialDelayString="${dieta.avisos.intervalo-sem-checkin:PT1H}")
	public void executarJob(){
		try { executar(); }
		catch (RuntimeException erro) { LoggerFactory.getLogger(AlertaSemCheckin.class).warn("Falha técnica no job de paciente sem check-in."); }
	}
	public int executar(){
		var lista=store.parados(relogio.instant(),DIAS);
		for(var p:lista)avisos.registrar(new NovoAviso(Perfil.NUTRICIONISTA,p.nutricionistaId(),TipoAviso.PACIENTE_SEM_CHECKIN,"/pacientes/"+p.pacienteId(),"sem-checkin:"+p.pacienteId()+":"+p.ultimaAtividade()));
		return lista.size();
	}
}
