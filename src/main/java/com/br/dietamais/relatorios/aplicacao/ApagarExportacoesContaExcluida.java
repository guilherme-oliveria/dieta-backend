package com.br.dietamais.relatorios.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.relatorios.infra.ExportacaoStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApagarExportacoesContaExcluida {
	private final ExportacaoStore store;private final Armazenamento armazenamento;
	public ApagarExportacoesContaExcluida(ExportacaoStore store,Armazenamento armazenamento){this.store=store;this.armazenamento=armazenamento;}
	@EventListener public void aoExcluirConta(ContaPacienteExcluida evento){
		for(var chave:store.apagarDoUsuario(evento.usuarioId()))armazenamento.apagar(chave);
	}
}
