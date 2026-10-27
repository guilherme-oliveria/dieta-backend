package com.br.dietamais.sync.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.sync.api.*;
import com.br.dietamais.sync.infra.MutacaoStore;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** {@code POST /sync}: cada mutação roda na sua própria transação e é idempotente por {@code idMutacao} (integracao.md). */
@Service
public class ServicoSyncEscrita {
	private final UsuarioAtual usuario;private final MutacaoStore mutacoes;private final Map<String,ManipuladorMutacao> manipuladores=new HashMap<>();private final TransactionTemplate tx;
	public ServicoSyncEscrita(UsuarioAtual usuario,MutacaoStore mutacoes,List<ManipuladorMutacao> manipuladores,PlatformTransactionManager transacoes){
		this.usuario=usuario;this.mutacoes=mutacoes;this.tx=new TransactionTemplate(transacoes);
		for(var m:manipuladores)if(this.manipuladores.put(m.entidade(),m)!=null)throw new IllegalArgumentException("Manipulador sync duplicado: "+m.entidade());
	}
	public EnviarMutacoesResposta enviar(EnviarMutacoesRequisicao lote){
		var contexto=usuario.obter();if(contexto.perfil()==Perfil.ADMIN)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		var resultados=new ArrayList<ResultadoMutacao>();
		for(var requisicao:lote.mutacoes())resultados.add(aplicar(new MutacaoSync(requisicao.idMutacao(),requisicao.entidade(),requisicao.operacao(),requisicao.id(),requisicao.versaoBase(),requisicao.dados()),contexto));
		return new EnviarMutacoesResposta(resultados);
	}
	private ResultadoMutacao aplicar(MutacaoSync m,ContextoUsuario contexto){
		var dono=contexto.usuarioId();var guardada=mutacoes.buscar(m.idMutacao());
		if(guardada!=null)return guardada.usuarioId().equals(dono)?guardada.resultado():ResultadoMutacao.rejeitada(m.idMutacao(),CodigoErro.CHAVE_IDEMPOTENCIA_REUTILIZADA.name(),null);
		var manipulador=manipuladores.get(m.entidade());
		ResultadoMutacao resultado;
		try{
			if(manipulador==null)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-03");
			resultado=tx.execute(status->{var r=manipulador.aplicar(m,contexto);mutacoes.guardar(dono,r);return r;});
		}catch(ExcecaoNegocio e){
			resultado=e.codigo()==CodigoErro.CONFLITO_VERSAO?new ResultadoMutacao(m.idMutacao(),ResultadoMutacao.Status.CONFLITO,null,e.codigo().name(),e.regra(),e.atual()):ResultadoMutacao.rejeitada(m.idMutacao(),e.codigo().name(),e.regra());
			final var guardar=resultado;tx.executeWithoutResult(status->mutacoes.guardar(dono,guardar));
		}
		return resultado;
	}
}
