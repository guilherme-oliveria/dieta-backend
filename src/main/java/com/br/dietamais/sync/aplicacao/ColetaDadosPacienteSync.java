package com.br.dietamais.sync.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.sync.api.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ColetaDadosPacienteSync implements ColetaDadosPaciente {
	private static final int PAGINA=500;
	private final ServicoSyncLeitura leitura;
	public ColetaDadosPacienteSync(ServicoSyncLeitura leitura){this.leitura=leitura;}
	@Override public List<MudancasFase1Resposta> coletar(ContextoUsuario paciente){
		if(paciente.perfil()!=Perfil.PACIENTE)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		var paginas=new ArrayList<MudancasFase1Resposta>();String cursor="0";
		for(int i=0;i<10_000;i++){
			var r=leitura.executar(paciente,cursor,PAGINA);paginas.add(r.mudancas());
			if(!r.temMais())return paginas;
			cursor=r.cursor();
		}
		throw new IllegalStateException("Coleta sem fim");
	}
}
