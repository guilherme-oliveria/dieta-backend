package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.notificacoes.api.AvisoResposta;
import com.br.dietamais.notificacoes.infra.AvisoStore;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fonte sync de avisos (BE-34): cada perfil recebe só os avisos do seu usuário; excluído devolve nulo (tombstone). */
@Component
public class FonteAvisosSync implements FonteSync<AvisoResposta> {
	private final DiarioSync diario;private final AvisoStore avisos;
	public FonteAvisosSync(DiarioSync diario,AvisoStore avisos){this.diario=diario;this.avisos=avisos;}
	@Override public ColecaoSync colecao(){return ColecaoSync.AVISOS;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	@Override public AvisoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){return avisos.doUsuario(referencia.id(),usuario.usuarioId(),usuario.perfil().name());}
}
