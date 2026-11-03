package com.br.dietamais.alimentos.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.infra.*;
import com.br.dietamais.comum.api.*;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Fontes sync da lista da nutri (BE-14): só a nutri dona recebe; registros sem visibilidade viram tombstone. */
public final class FontesAlimentosSync {
	private FontesAlimentosSync(){}

	@Component
	public static class Grupos implements FonteSync<GrupoAlimentoResposta> {
		private final DiarioSync diario;private final CatalogoAlimentosStore grupos;
		public Grupos(DiarioSync diario,CatalogoAlimentosStore grupos){this.diario=diario;this.grupos=grupos;}
		@Override public ColecaoSync colecao(){return ColecaoSync.GRUPOS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public GrupoAlimentoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){return grupos.grupo(referencia.id(),usuario.exigirNutricionista(),false);}
	}

	@Component
	public static class Alimentos implements FonteSync<AlimentoResposta> {
		private final DiarioSync diario;private final ListasAlimentosStore listas;
		public Alimentos(DiarioSync diario,ListasAlimentosStore listas){this.diario=diario;this.listas=listas;}
		@Override public ColecaoSync colecao(){return ColecaoSync.ALIMENTOS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public AlimentoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){return listas.alimento(referencia.id(),usuario.exigirNutricionista(),false);}
	}

	/** O id do envelope é o do vínculo; o paciente atual vem do vínculo (pode ter sido trocado no aceite). */
	@Component
	public static class Listas implements FonteSync<ListaSubstituicaoResposta> {
		private final DiarioSync diario;private final ListaSubstituicaoEfetiva efetiva;private final JdbcTemplate jdbc;private final AuditoriaClinica auditoria;
		public Listas(DiarioSync diario,ListaSubstituicaoEfetiva efetiva,JdbcTemplate jdbc,AuditoriaClinica auditoria){this.diario=diario;this.efetiva=efetiva;this.jdbc=jdbc;this.auditoria=auditoria;}
		@Override public ColecaoSync colecao(){return ColecaoSync.LISTAS;}
		@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
		@Override public ListaSubstituicaoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
			var nutri=usuario.exigirNutricionista();
			var paciente=jdbc.query("select paciente_id from vinculo where id=? and nutricionista_id=? and excluido_em is null",(rs,n)->rs.getObject(1,UUID.class),referencia.id(),nutri).stream().findFirst().orElse(null);
			if(paciente==null)return null;
			var lista=efetiva.consultar(referencia.id(),paciente,nutri);
			auditoria.leitura(new ReferenciaClinica("lista_substituicao",referencia.id(),nutri,paciente,usuario.organizacaoId()));return lista;
		}
	}
}
