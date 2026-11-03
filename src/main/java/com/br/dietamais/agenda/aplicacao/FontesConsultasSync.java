package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.api.ConsultaResposta;
import com.br.dietamais.agenda.infra.ConsultaStore;
import com.br.dietamais.comum.api.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fonte sync de consultas (BE-23): nutri e paciente recebem a mesma consulta (canceladas continuam, com status). */
@Component
public class FontesConsultasSync implements FonteSync<ConsultaResposta> {
	private final DiarioSync diario;private final ConsultaStore consultas;
	public FontesConsultasSync(DiarioSync diario,ConsultaStore consultas){this.diario=diario;this.consultas=consultas;}
	@Override public ColecaoSync colecao(){return ColecaoSync.CONSULTAS;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	@Override public ConsultaResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
		if(usuario.perfil()==Perfil.NUTRICIONISTA)return consultas.daNutri(referencia.id(),usuario.exigirNutricionista(),false);
		return consultas.doPaciente(referencia.id(),usuario.exigirPaciente());
	}
}
