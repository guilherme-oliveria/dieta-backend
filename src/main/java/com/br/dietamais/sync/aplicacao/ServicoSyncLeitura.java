package com.br.dietamais.sync.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.sync.api.*;
import com.br.dietamais.sync.dominio.CursorSync;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ServicoSyncLeitura {
	private final UsuarioAtual usuario;private final DiarioSync diario;private final List<FonteSync<?>> fontes;
	public ServicoSyncLeitura(UsuarioAtual usuario,DiarioSync diario,List<FonteSync<?>> fontes) {
		this.usuario=usuario;this.diario=diario;this.fontes=List.copyOf(fontes);
		var colecoes=EnumSet.noneOf(ColecaoSync.class);
		for(var fonte:fontes)if(fonte.colecao()==null||!colecoes.add(fonte.colecao()))throw new IllegalArgumentException("Fonte sync inválida ou duplicada");
		if(colecoes.size()!=ColecaoSync.values().length)throw new IllegalArgumentException("Fontes sync ausentes");
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public SyncResposta executar(String valor,int limite) {
		var contexto=usuario.obter();if(contexto.perfil()==Perfil.ADMIN)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		long cursor=CursorSync.ler(valor),marco=diario.marcoAtual();CursorSync.validar(cursor,marco,limite);
		var candidatos=new ArrayList<Entrada>();var sequencias=new HashSet<Long>();
		for(var fonte:fontes)for(var referencia:fonte.mudancasDesde(cursor,contexto,limite+1)) {
			if(referencia.seq()<=cursor||referencia.seq()>marco||!sequencias.add(referencia.seq()))throw new IllegalStateException("Revisão sync inconsistente");
			candidatos.add(new Entrada(fonte,referencia));
		}
		candidatos.sort(Comparator.comparingLong(entrada->entrada.referencia().seq()));boolean mais=candidatos.size()>limite;
		var pacientes=new ArrayList<MudancaSyncResposta<PacienteResposta>>();var vinculos=new ArrayList<MudancaSyncResposta<VinculoResposta>>();var nutris=new ArrayList<MudancaSyncResposta<NutricionistaResumoResposta>>();
		long ultimo=cursor;
		for(var entrada:candidatos.subList(0,Math.min(limite,candidatos.size()))) {
			var referencia=entrada.referencia();Object dados=entrada.fonte().carregar(referencia,contexto);ultimo=referencia.seq();
			switch(entrada.fonte().colecao()) {
				case PACIENTES->pacientes.add(mudanca(referencia,dados,PacienteResposta.class));
				case VINCULOS->vinculos.add(mudanca(referencia,dados,VinculoResposta.class));
				case NUTRIS->nutris.add(mudanca(referencia,dados,NutricionistaResumoResposta.class));
			}
		}
		return new SyncResposta(Long.toString(mais?ultimo:marco),mais,new MudancasFase1Resposta(pacientes,vinculos,nutris));
	}
	private <T> MudancaSyncResposta<T> mudanca(ReferenciaSync referencia,Object dados,Class<T> tipo){return new MudancaSyncResposta<>(referencia.id(),referencia.seq(),dados==null,tipo.cast(dados));}
	private record Entrada(FonteSync<?> fonte,ReferenciaSync referencia){}
}
