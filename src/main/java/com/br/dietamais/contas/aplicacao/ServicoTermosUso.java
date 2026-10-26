package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.infra.FonteTermosUso;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class ServicoTermosUso implements TermosUso {
	private final FonteTermosUso fonte;private final Clock relogio;
	public ServicoTermosUso(FonteTermosUso fonte,Clock relogio){this.fonte=fonte;this.relogio=relogio;}
	@Override public TermosUsoResposta consultar() {
		var doc=fonte.obter();var agora=relogio.instant();boolean habilitado=doc.habilitado(agora);
		return new TermosUsoResposta(doc.versao(),doc.status(),doc.aprovado(agora),habilitado,habilitado,doc.aprovadoEm(),doc.publicadoEm(),doc.conteudo(),"/legal/terms-of-use");
	}
	@Override public void validar(Boolean aceitou,String versao) {
		var termos=consultar();
		if(!termos.cadastroHabilitado())throw new ExcecaoNegocio(CodigoErro.TERMOS_INDISPONIVEIS,"RN-CON-09","O cadastro aguarda aprovação e publicação dos termos de uso.",java.util.List.of(),null);
		if(!Boolean.TRUE.equals(aceitou)||!termos.versao().equals(versao))throw new ExcecaoNegocio(CodigoErro.TERMOS_NAO_ACEITOS,"RN-CON-09","Leia e aceite a versão publicada dos termos de uso.",java.util.List.of(),null);
	}
}
