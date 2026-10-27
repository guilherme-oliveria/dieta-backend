package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.pacientes.infra.FichaPacienteStore;
import com.br.dietamais.comum.api.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConsultarFichaParaListaPropria implements ConsultarFichaParaLista {
	private final UsuarioAtual usuario;private final FichaPacienteStore fichas;
	public ConsultarFichaParaListaPropria(UsuarioAtual usuario,FichaPacienteStore fichas){this.usuario=usuario;this.fichas=fichas;}
	@Override public FichaParaLista consultar(UUID paciente,boolean bloquear){
		var ficha=fichas.consultar(paciente,usuario.obter().exigirNutricionista(),bloquear);
		if(ficha==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return new FichaParaLista(ficha.vinculoId(),ficha.id(),ficha.status());
	}
}
