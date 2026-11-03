package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.notificacoes.infra.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ProcessarEmails {
	private final JdbcTemplate jdbc;
	private final CifradorEmail cifra;
	private final JsonMapper mapper;
	private final RenderizadorEmail renderizador;
	private final EnviadorEmail enviador;
	private final Clock relogio;
	private final TransactionTemplate transacao;
	private final boolean habilitado;
	public ProcessarEmails(JdbcTemplate jdbc,CifradorEmail cifra,JsonMapper mapper,RenderizadorEmail renderizador,
			EnviadorEmail enviador,Clock relogio,PlatformTransactionManager transacoes,
			@Value("${dieta.notificacoes.envio-habilitado:false}") boolean habilitado) {
		this.jdbc=jdbc;this.cifra=cifra;this.mapper=mapper;this.renderizador=renderizador;
		this.enviador=enviador;this.relogio=relogio;this.transacao=new TransactionTemplate(transacoes);this.habilitado=habilitado;
	}
	@Scheduled(fixedDelayString="${dieta.notificacoes.intervalo-envio:PT30S}",initialDelayString="${dieta.notificacoes.intervalo-envio:PT30S}")
	public void executarJob() {
		try {
			if (!habilitado) { expirarPayloads(); return; }
			for (int i=0;i<10 && processarProxima();i++) { }
		}
		catch (RuntimeException erro) { LoggerFactory.getLogger(ProcessarEmails.class).warn("Falha técnica no job de e-mail."); }
	}
	private void expirarPayloads() {
		jdbc.update("update notificacao_pendente set status='EXPIRADA',payload_cifrado=null where status='PENDENTE' and expira_em<=?",Timestamp.from(relogio.instant()));
	}
	public boolean processarProxima() {
		return Boolean.TRUE.equals(transacao.execute(status -> {
			var agora=relogio.instant();var instante=Timestamp.from(agora);
			expirarPayloads();
			var candidatos=jdbc.query("""
				select id,tipo,payload_cifrado,tentativas from notificacao_pendente
				where status='PENDENTE' and proxima_tentativa_em<=? and expira_em>?
				order by proxima_tentativa_em,id limit 1 for update skip locked
				""",(rs,n)->new Pendente(rs.getObject("id",UUID.class),rs.getString("tipo"),rs.getString("payload_cifrado"),rs.getInt("tentativas")),instante,instante);
			if(candidatos.isEmpty())return false;
			var pendente=candidatos.getFirst();
			try {
				var email=mapper.readValue(cifra.decifrar(pendente.id(),pendente.tipo(),pendente.payload()),EmailTransacional.class);
				enviador.enviar(renderizador.renderizar(email));
				jdbc.update("update notificacao_pendente set status='ENVIADA',tentativas=tentativas+1,enviado_em=?,payload_cifrado=null where id=?",Timestamp.from(relogio.instant()),pendente.id());
			} catch (RuntimeException erro) {
				int tentativas=pendente.tentativas()+1;
				boolean esgotou=tentativas>=5;
				jdbc.update("update notificacao_pendente set tentativas=?,status=?,proxima_tentativa_em=?,payload_cifrado=case when ? then null else payload_cifrado end where id=?",
						tentativas,esgotou?"FALHA":"PENDENTE",Timestamp.from(relogio.instant().plusSeconds(30L << (tentativas-1))),esgotou,pendente.id());
				LoggerFactory.getLogger(ProcessarEmails.class).warn("Falha de envio de e-mail; notificacaoId={}; tentativa={}",pendente.id(),tentativas);
			}
			return true;
		}));
	}
	private record Pendente(UUID id,String tipo,String payload,int tentativas) {
		@Override public String toString(){return "Pendente[conteúdo protegido]";}
	}
}
