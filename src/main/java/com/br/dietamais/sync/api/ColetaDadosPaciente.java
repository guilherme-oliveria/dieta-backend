package com.br.dietamais.sync.api;

import com.br.dietamais.comum.api.ContextoUsuario;
import java.util.List;

/** Tudo o que o sync entregaria ao paciente desde o início (cursor 0), em páginas. Base da exportação de dados (RN-LGPD-02). */
public interface ColetaDadosPaciente {
	List<MudancasFase1Resposta> coletar(ContextoUsuario paciente);
}
