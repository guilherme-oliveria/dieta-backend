package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.dietas.api.DietaPublicada;
import com.br.dietamais.notificacoes.api.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** RN-NOT-01: avisa o paciente da dieta nova, só depois do commit da publicação. */
@Component
public class AvisarNovaDieta {
	private final RegistrarAviso avisos;
	public AvisarNovaDieta(RegistrarAviso avisos){this.avisos=avisos;}
	@TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT,fallbackExecution=true)
	void aoPublicar(DietaPublicada e){
		avisos.registrar(new NovoAviso(Perfil.PACIENTE,e.pacienteId(),TipoAviso.NOVA_DIETA,"/dieta","dieta:"+e.snapshotId()));
	}
}
