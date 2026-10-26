package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.PacienteResposta;
import com.br.dietamais.pacientes.infra.FichaPacienteStore;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FontePacientesSync implements FonteSync<PacienteResposta> {
	private final DiarioSync diario;private final FichaPacienteStore fichas;private final AuditoriaClinica auditoria;
	public FontePacientesSync(DiarioSync diario,FichaPacienteStore fichas,AuditoriaClinica auditoria){this.diario=diario;this.fichas=fichas;this.auditoria=auditoria;}
	@Override public ColecaoSync colecao(){return ColecaoSync.PACIENTES;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	@Override public PacienteResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario) {
		var nutri=usuario.exigirNutricionista();var dados=fichas.consultar(referencia.id(),nutri,false);
		auditoria.leitura(new ReferenciaClinica("paciente_sync",referencia.id(),nutri,referencia.id(),usuario.organizacaoId()));return dados;
	}
}
