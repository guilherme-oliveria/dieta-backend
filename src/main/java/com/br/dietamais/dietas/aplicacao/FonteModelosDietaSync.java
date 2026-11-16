package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.ModeloDietaResposta;
import com.br.dietamais.dietas.infra.ModeloDietaStore;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fonte sync dos modelos de dieta da nutri (BE-52). Os do sistema não entram: vêm por {@code GET /eu/nutri/modelos-dieta}. */
@Component
public class FonteModelosDietaSync implements FonteSync<ModeloDietaResposta> {
	private final DiarioSync diario;private final ModeloDietaStore store;private final GerenciarModelosDieta modelos;
	public FonteModelosDietaSync(DiarioSync diario,ModeloDietaStore store,GerenciarModelosDieta modelos){this.diario=diario;this.store=store;this.modelos=modelos;}
	@Override public ColecaoSync colecao(){return ColecaoSync.MODELOS_DIETA;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	/** Modelo excluído ou que não é dela devolve {@code null} (tombstone). */
	@Override public ModeloDietaResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
		var linha=store.daNutri(referencia.id(),usuario.exigirNutricionista(),false);
		return linha==null?null:modelos.resposta(linha);
	}
}
