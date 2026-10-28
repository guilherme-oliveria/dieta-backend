package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.notificacoes.api.*;
import java.sql.Timestamp;
import java.time.Clock;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicoRegistrarAviso implements RegistrarAviso {
	private final JdbcTemplate jdbc;private final Clock relogio;private final TransactionTemplate transacao;
	public ServicoRegistrarAviso(JdbcTemplate jdbc,Clock relogio,PlatformTransactionManager transacoes){
		this.jdbc=jdbc;this.relogio=relogio;
		this.transacao=new TransactionTemplate(transacoes);this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}
	/** Transação própria: um aviso que falha não desfaz a dieta publicada nem o job que o chamou. */
	@Override public void registrar(NovoAviso aviso){
		try { transacao.executeWithoutResult(s->gravar(aviso)); }
		catch (RuntimeException erro) { LoggerFactory.getLogger(ServicoRegistrarAviso.class).warn("Não foi possível registrar o aviso; tipo={}",aviso.tipo()); }
	}
	private void gravar(NovoAviso a){
		var tipo=a.tipo();
		if(a.perfil()!=Perfil.PACIENTE&&a.perfil()!=Perfil.NUTRICIONISTA||tipo.perfil()!=null&&tipo.perfil()!=a.perfil())throw new IllegalArgumentException("Perfil incompatível com o tipo de aviso.");
		var tabela=a.perfil()==Perfil.PACIENTE?"paciente":"nutricionista";
		var agora=Timestamp.from(relogio.instant());
		// paciente sem conta (convite pendente) não tem usuário: o select não devolve linha e nada é gravado
		jdbc.update("insert into aviso(usuario_id,tipo,titulo,corpo,rota,chave_dedup,push_proxima_em,criado_em,atualizado_em) "
			+"select usuario_id,?,?,?,?,?,?,?,? from "+tabela+" where id=? and usuario_id is not null "
			+"on conflict (usuario_id,chave_dedup) where chave_dedup is not null do nothing",
			tipo.name(),tipo.titulo(),tipo.corpo(),a.rota(),a.chaveDedup(),agora,agora,agora,a.destinatarioId());
	}
}
