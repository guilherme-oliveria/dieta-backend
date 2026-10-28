package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.AvaliacaoResposta;
import com.br.dietamais.avaliacoes.infra.AvaliacaoStore;
import com.br.dietamais.comum.api.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fonte sync de avaliações (BE-19): a nutri recebe as próprias; o paciente só as compartilhadas (tombstone quando deixa de compartilhar). */
@Component
public class FontesAvaliacoesSync implements FonteSync<AvaliacaoResposta> {
	private final DiarioSync diario;private final AvaliacaoStore avaliacoes;private final AuditoriaClinica auditoria;
	public FontesAvaliacoesSync(DiarioSync diario,AvaliacaoStore avaliacoes,AuditoriaClinica auditoria){this.diario=diario;this.avaliacoes=avaliacoes;this.auditoria=auditoria;}
	@Override public ColecaoSync colecao(){return ColecaoSync.AVALIACOES;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	@Override public AvaliacaoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario){
		if(usuario.perfil()==Perfil.NUTRICIONISTA){
			var nutri=usuario.exigirNutricionista();var a=avaliacoes.daNutri(referencia.id(),nutri);
			if(a!=null)auditoria.leitura(new ReferenciaClinica("avaliacao",a.id(),nutri,a.pacienteId(),usuario.organizacaoId()));
			return a;
		}
		return avaliacoes.compartilhadaDoPaciente(referencia.id(),usuario.exigirPaciente());
	}
}
