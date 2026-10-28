package com.br.dietamais.sync.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.sync.api.*;
import com.br.dietamais.sync.infra.MutacaoStore;
import com.br.dietamais.sync.dominio.ConteudoMutacao;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** {@code POST /sync}: cada mutação roda na sua própria transação e é idempotente por {@code idMutacao} (integracao.md). */
@Service
public class ServicoSyncEscrita {
	private final UsuarioAtual usuario;private final MutacaoStore mutacoes;private final Map<String,ManipuladorMutacao> manipuladores=new HashMap<>();private final TransactionTemplate tx;
	private final AutenticadorConteudo autenticador;private final JsonMapper mapper;
	public ServicoSyncEscrita(UsuarioAtual usuario,MutacaoStore mutacoes,List<ManipuladorMutacao> manipuladores,PlatformTransactionManager transacoes,AutenticadorConteudo autenticador,JsonMapper mapper){
		this.usuario=usuario;this.mutacoes=mutacoes;this.tx=new TransactionTemplate(transacoes);this.tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);this.autenticador=autenticador;this.mapper=mapper;
		for(var m:manipuladores)if(this.manipuladores.put(m.entidade(),m)!=null)throw new IllegalArgumentException("Manipulador sync duplicado: "+m.entidade());
	}
	public EnviarMutacoesResposta enviar(EnviarMutacoesRequisicao lote){
		var contexto=usuario.obter();if(contexto.perfil()==Perfil.ADMIN)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		var resultados=new ArrayList<ResultadoMutacao>();
		for(var requisicao:lote.mutacoes())resultados.add(aplicar(new MutacaoSync(requisicao.idMutacao(),requisicao.entidade(),requisicao.operacao(),requisicao.id(),requisicao.versaoBase(),requisicao.dados()),contexto));
		return new EnviarMutacoesResposta(resultados);
	}
	private ResultadoMutacao aplicar(MutacaoSync m,ContextoUsuario contexto){
		String fingerprint=autenticador.autenticarRequisicao(ConteudoMutacao.canonico(m,mapper)).orElseThrow(()->new IllegalStateException("Configure IDEMPOTENCIA_CHAVE antes de usar o sync de escrita"));
		try{
			return tx.execute(status->{
				mutacoes.bloquear(m.idMutacao());var guardada=mutacoes.buscar(m.idMutacao());if(guardada!=null)return replay(m,contexto,fingerprint,guardada);
				var manipulador=manipuladores.get(m.entidade());if(manipulador==null)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-03");
				var resultado=manipulador.aplicar(m,contexto);guardar(contexto,fingerprint,resultado);return resultado;
			});
		}catch(ExcecaoNegocio e){
			var resultado=e.codigo()==CodigoErro.CONFLITO_VERSAO?new ResultadoMutacao(m.idMutacao(),ResultadoMutacao.Status.CONFLITO,null,e.codigo().name(),e.regra(),e.atual()):ResultadoMutacao.rejeitada(m.idMutacao(),e.codigo().name(),e.regra());
			// A operação falhou e foi desfeita; outra requisição pode ter confirmado a chave nesse intervalo.
			return tx.execute(status->{mutacoes.bloquear(m.idMutacao());var guardada=mutacoes.buscar(m.idMutacao());if(guardada!=null)return replay(m,contexto,fingerprint,guardada);guardar(contexto,fingerprint,resultado);return resultado;});
		}
	}
	private ResultadoMutacao replay(MutacaoSync m,ContextoUsuario contexto,String fingerprint,MutacaoStore.Guardada guardada){
		if(guardada.usuarioId().equals(contexto.usuarioId())&&guardada.perfil()==contexto.perfil()&&guardada.fingerprint()!=null&&java.security.MessageDigest.isEqual(guardada.fingerprint().getBytes(java.nio.charset.StandardCharsets.US_ASCII),fingerprint.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))return guardada.resultado();
		return ResultadoMutacao.rejeitada(m.idMutacao(),CodigoErro.CHAVE_IDEMPOTENCIA_REUTILIZADA.name(),null);
	}
	private void guardar(ContextoUsuario contexto,String fingerprint,ResultadoMutacao resultado){if(!mutacoes.guardar(contexto.usuarioId(),contexto.perfil(),fingerprint,resultado))throw new IllegalStateException("A reserva da mutação foi alterada; operação desfeita");}
}
