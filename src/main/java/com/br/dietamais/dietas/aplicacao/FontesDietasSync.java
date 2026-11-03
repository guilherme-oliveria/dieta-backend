package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.infra.DietaStore;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Fontes sync de dietas (BE-14): nutri recebe rascunhos e publicadas próprias; paciente recebe snapshots e a dieta em uso. */
public final class FontesDietasSync {
	private FontesDietasSync(){}

	@Component
	public static class Dietas implements FonteSync<DietaResposta> {
		private final DiarioSync diario;private final DietaStore dietas;private final MontadorDieta montador;private final AuditoriaClinica auditoria;
		public Dietas(DiarioSync diario,DietaStore dietas,MontadorDieta montador,AuditoriaClinica auditoria){this.diario=diario;this.dietas=dietas;this.montador=montador;this.auditoria=auditoria;}
		@Override public ColecaoSync colecao(){return ColecaoSync.DIETAS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public DietaResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
			var nutri=usuario.exigirNutricionista();var dieta=dietas.dieta(referencia.id(),nutri,false);if(dieta==null)return null;
			auditoria.leitura(new ReferenciaClinica("dieta",dieta.id(),nutri,dieta.pacienteId(),usuario.organizacaoId()));
			return montador.montar(List.of(dieta),nutri).get(0);
		}
	}

	@Component
	public static class Snapshots implements FonteSync<DietaSnapshotResposta> {
		private final DiarioSync diario;private final DietaStore dietas;private final AuditoriaClinica auditoria;private final JsonMapper mapper;
		public Snapshots(DiarioSync diario,DietaStore dietas,AuditoriaClinica auditoria,JsonMapper mapper){this.diario=diario;this.dietas=dietas;this.auditoria=auditoria;this.mapper=mapper;}
		@Override public ColecaoSync colecao(){return ColecaoSync.SNAPSHOTS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public DietaSnapshotResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
			var paciente=usuario.exigirPaciente();
			var recebido=dietas.snapshotsDoPaciente(paciente).stream().filter(r->r.id().equals(referencia.id())).findFirst().orElse(null);
			if(recebido==null)return null;
			auditoria.leitura(new ReferenciaClinica("dieta_snapshot",recebido.id(),recebido.nutricionistaId(),paciente,usuario.organizacaoId()));
			return ConsultarDietasPaciente.doPaciente(mapper.readValue(recebido.conteudo(),DietaSnapshotResposta.class),paciente);
		}
	}

	/** O id do envelope é o do paciente; sem dieta candidata devolve tombstone. */
	@Component
	public static class EmUso implements FonteSync<DietaEmUsoResposta> {
		private final DiarioSync diario;private final ConsultarDietasPaciente dietas;
		public EmUso(DiarioSync diario,ConsultarDietasPaciente dietas){this.diario=diario;this.dietas=dietas;}
		@Override public ColecaoSync colecao(){return ColecaoSync.DIETA_EM_USO;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public DietaEmUsoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){return dietas.emUsoAtual(usuario.exigirPaciente());}
	}
}
