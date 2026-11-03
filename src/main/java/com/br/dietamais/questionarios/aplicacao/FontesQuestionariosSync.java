package com.br.dietamais.questionarios.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.api.*;
import com.br.dietamais.questionarios.infra.QuestionarioStore;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fontes sync da anamnese (BE-21/22): modelos só da nutri; questionários (com respostas) para a nutri dona e para o paciente. */
public final class FontesQuestionariosSync {
	private FontesQuestionariosSync(){}

	@Component
	public static class Modelos implements FonteSync<ModeloQuestionarioResposta> {
		private final DiarioSync diario;private final QuestionarioStore store;
		public Modelos(DiarioSync diario,QuestionarioStore store){this.diario=diario;this.store=store;}
		@Override public ColecaoSync colecao(){return ColecaoSync.MODELOS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public ModeloQuestionarioResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){return store.modelo(referencia.id(),usuario.exigirNutricionista());}
	}

	@Component
	public static class Questionarios implements FonteSync<QuestionarioResposta> {
		private final DiarioSync diario;private final QuestionarioStore store;private final AuditoriaClinica auditoria;
		public Questionarios(DiarioSync diario,QuestionarioStore store,AuditoriaClinica auditoria){this.diario=diario;this.store=store;this.auditoria=auditoria;}
		@Override public ColecaoSync colecao(){return ColecaoSync.QUESTIONARIOS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public QuestionarioResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
			if(usuario.perfil()==Perfil.NUTRICIONISTA){
				var nutri=usuario.exigirNutricionista();var q=store.daNutri(referencia.id(),nutri);
				if(q!=null)auditoria.leitura(new ReferenciaClinica("questionario",q.id(),nutri,q.pacienteId(),usuario.organizacaoId()));
				return q;
			}
			return store.doPacienteUm(referencia.id(),usuario.exigirPaciente());
		}
	}
}
