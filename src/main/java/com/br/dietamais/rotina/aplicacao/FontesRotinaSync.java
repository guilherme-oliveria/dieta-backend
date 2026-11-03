package com.br.dietamais.rotina.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.rotina.api.*;
import com.br.dietamais.rotina.infra.RotinaStore;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fontes sync da rotina (BE-15): o paciente recebe os próprios registros; a nutri do vínculo ativo vê check-ins e pesos (RN-ROT-02). */
public final class FontesRotinaSync {
	private FontesRotinaSync(){}

	@Component
	public static class Checkins implements FonteSync<CheckinResposta> {
		private final DiarioSync diario;private final RotinaStore rotina;private final AuditoriaClinica auditoria;
		public Checkins(DiarioSync diario,RotinaStore rotina,AuditoriaClinica auditoria){this.diario=diario;this.rotina=rotina;this.auditoria=auditoria;}
		@Override public ColecaoSync colecao(){return ColecaoSync.CHECKINS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public CheckinResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
			boolean nutri=usuario.perfil()==Perfil.NUTRICIONISTA;
			var dados=nutri?rotina.checkinDoVinculo(referencia.id(),usuario.exigirNutricionista()):rotina.checkin(referencia.id(),usuario.exigirPaciente());
			if(dados!=null&&nutri)auditoria.leitura(new ReferenciaClinica("checkin",dados.id(),usuario.exigirNutricionista(),dados.pacienteId(),usuario.organizacaoId()));
			return dados;
		}
	}

	@Component
	public static class Agua implements FonteSync<AguaResposta> {
		private final DiarioSync diario;private final RotinaStore rotina;
		public Agua(DiarioSync diario,RotinaStore rotina){this.diario=diario;this.rotina=rotina;}
		@Override public ColecaoSync colecao(){return ColecaoSync.AGUA;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public AguaResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){return rotina.agua(referencia.id(),usuario.exigirPaciente());}
	}

	@Component
	public static class Pesos implements FonteSync<PesoResposta> {
		private final DiarioSync diario;private final RotinaStore rotina;private final AuditoriaClinica auditoria;
		public Pesos(DiarioSync diario,RotinaStore rotina,AuditoriaClinica auditoria){this.diario=diario;this.rotina=rotina;this.auditoria=auditoria;}
		@Override public ColecaoSync colecao(){return ColecaoSync.PESOS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public PesoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
			boolean nutri=usuario.perfil()==Perfil.NUTRICIONISTA;
			var dados=nutri?rotina.pesoDoPacienteDaNutri(referencia.id(),usuario.exigirNutricionista()):rotina.peso(referencia.id(),usuario.exigirPaciente());
			if(dados!=null&&nutri)auditoria.leitura(new ReferenciaClinica("registro_peso",dados.id(),usuario.exigirNutricionista(),dados.pacienteId(),usuario.organizacaoId()));
			return dados;
		}
	}
}
