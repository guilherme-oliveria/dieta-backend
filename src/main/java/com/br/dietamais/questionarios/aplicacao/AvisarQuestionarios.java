package com.br.dietamais.questionarios.aplicacao;

import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.questionarios.api.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** RN-NOT-01: questionário enviado (para o paciente) e respondido (para a nutri), só depois do commit. */
@Component
public class AvisarQuestionarios {
	private final RegistrarAviso avisos;
	public AvisarQuestionarios(RegistrarAviso avisos){this.avisos=avisos;}
	@TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT,fallbackExecution=true)
	void aoEnviar(QuestionarioEnviado e){
		avisos.registrar(new NovoAviso(Perfil.PACIENTE,e.pacienteId(),TipoAviso.QUESTIONARIO_ENVIADO,"/questionarios/"+e.questionarioId(),"questionario-enviado:"+e.questionarioId()));
	}
	@TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT,fallbackExecution=true)
	void aoResponder(QuestionarioRespondido e){
		avisos.registrar(new NovoAviso(Perfil.NUTRICIONISTA,e.nutricionistaId(),TipoAviso.QUESTIONARIO_RESPONDIDO,"/pacientes/"+e.pacienteId(),"questionario-respondido:"+e.questionarioId()));
	}
}
