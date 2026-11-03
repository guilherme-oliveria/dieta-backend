package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.ProtocolosNutriResposta;
import com.br.dietamais.avaliacoes.infra.ProtocolosNutriStore;
import com.br.dietamais.comum.api.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fonte sync de "Meus protocolos" (BE-42): só a própria nutri recebe, e só o registro dela. */
@Component
public class FonteProtocolosSync implements FonteSync<ProtocolosNutriResposta> {
	private final DiarioSync diario;private final ProtocolosNutriStore store;
	public FonteProtocolosSync(DiarioSync diario,ProtocolosNutriStore store){this.diario=diario;this.store=store;}
	@Override public ColecaoSync colecao(){return ColecaoSync.PROTOCOLOS;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	@Override public ProtocolosNutriResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
		var nutri=usuario.exigirNutricionista();
		return referencia.id().equals(nutri)?store.consultar(nutri,false):null;
	}
}
