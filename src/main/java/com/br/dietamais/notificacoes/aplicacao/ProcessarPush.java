package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.notificacoes.api.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envia o push dos avisos pendentes. Várias instâncias podem rodar juntas: {@code for update skip locked} garante que
 * cada aviso é pego por uma só (por isso não há ShedLock) e a chave de dedup impede avisos repetidos.
 */
@Service
public class ProcessarPush {
	static final int MAX_TENTATIVAS=5;
	static final Duration VALIDADE=Duration.ofHours(24);
	private final JdbcTemplate jdbc;private final EnviadorPush enviador;private final Clock relogio;private final TransactionTemplate transacao;private final boolean habilitado;
	public ProcessarPush(JdbcTemplate jdbc,EnviadorPush enviador,Clock relogio,PlatformTransactionManager transacoes,@Value("${dieta.push.habilitado:false}") boolean habilitado){
		this.jdbc=jdbc;this.enviador=enviador;this.relogio=relogio;this.transacao=new TransactionTemplate(transacoes);this.habilitado=habilitado;
	}
	@Scheduled(fixedDelayString="${dieta.push.intervalo-envio:PT30S}",initialDelayString="${dieta.push.intervalo-envio:PT30S}")
	public void executarJob(){
		if(!habilitado)return;
		try { for(int i=0;i<50&&processarProximo();i++){} }
		catch (RuntimeException erro) { LoggerFactory.getLogger(ProcessarPush.class).warn("Falha técnica no job de push."); }
	}

	private record Pendente(UUID id,UUID usuario,String tipo,String titulo,String corpo,String rota,int tentativas,Instant criadoEm) {}
	private record Preferencia(boolean desligado,boolean silencio,LocalTime inicio,LocalTime fim,ZoneId fuso) {}

	public boolean processarProximo(){
		return Boolean.TRUE.equals(transacao.execute(s->{
			var agora=relogio.instant();
			var lista=jdbc.query("select id,usuario_id,tipo,titulo,corpo,rota,push_tentativas,criado_em from aviso where push_status='PENDENTE' and push_proxima_em<=? and excluido_em is null order by push_proxima_em,id limit 1 for update skip locked",
				(rs,i)->new Pendente(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getInt(7),rs.getTimestamp(8).toInstant()),Timestamp.from(agora));
			if(lista.isEmpty())return false;
			var a=lista.getFirst();
			if(a.criadoEm().plus(VALIDADE).isBefore(agora)){encerrar(a,"EXPIRADO",agora);return true;}
			var pref=preferencia(a);
			if(pref.desligado()){encerrar(a,"DESLIGADO",agora);return true;}
			if(pref.silencio()){
				var fimSilencio=fimDoSilencio(pref,agora);
				if(fimSilencio!=null){
					if(a.criadoEm().plus(VALIDADE).isBefore(fimSilencio)){encerrar(a,"EXPIRADO",agora);return true;}
					jdbc.update("update aviso set push_proxima_em=? where id=?",Timestamp.from(fimSilencio),a.id());return true;
				}
			}
			var tokens=jdbc.query("select id,token from dispositivo where usuario_id=? and plataforma<>'DESKTOP'",(rs,i)->Map.entry(rs.getObject(1,UUID.class),rs.getString(2)),a.usuario());
			if(tokens.isEmpty()){encerrar(a,"SEM_DISPOSITIVO",agora);return true;}
			boolean enviou=false,falhou=false;
			for(var t:tokens){
				var r=enviador.enviar(new MensagemPush(t.getValue(),a.tipo(),a.titulo(),a.corpo(),a.rota()));
				switch(r){
					case ENVIADO->enviou=true;
					case TOKEN_INVALIDO->jdbc.update("delete from dispositivo where id=?",t.getKey());
					case FALHA_TEMPORARIA->falhou=true;
				}
			}
			if(enviou)encerrar(a,"ENVIADO",agora);
			else if(!falhou)encerrar(a,"SEM_DISPOSITIVO",agora);
			else repetir(a,agora);
			return true;
		}));
	}
	private void encerrar(Pendente a,String status,Instant agora){
		jdbc.update("update aviso set push_status=?,push_enviado_em=? where id=?",status,"ENVIADO".equals(status)?Timestamp.from(agora):null,a.id());
	}
	private void repetir(Pendente a,Instant agora){
		int tentativas=a.tentativas()+1;
		if(tentativas>=MAX_TENTATIVAS){
			jdbc.update("update aviso set push_status='FALHA',push_tentativas=? where id=?",tentativas,a.id());
			LoggerFactory.getLogger(ProcessarPush.class).warn("Push desistiu após {} tentativas; avisoId={}",tentativas,a.id());return;
		}
		jdbc.update("update aviso set push_tentativas=?,push_proxima_em=? where id=?",tentativas,Timestamp.from(agora.plusSeconds(30L<<(tentativas-1))),a.id());
	}
	private Preferencia preferencia(Pendente a){
		var fuso=jdbc.queryForObject("select fuso from usuario where id=?",String.class,a.usuario());
		var zona=ZoneId.of(fuso);
		var p=jdbc.query("select tipos_desligados @> to_jsonb(?::text) ,silencio_ativo,silencio_inicio,silencio_fim from preferencia_notificacao where usuario_id=?",
			(rs,i)->new Preferencia(rs.getBoolean(1),rs.getBoolean(2),rs.getTime(3).toLocalTime(),rs.getTime(4).toLocalTime(),zona),preferenciaDe(a.tipo()),a.usuario());
		// RN-NOT-03: sem linha valem os padrões (tudo ligado, silêncio 22:00–07:00)
		return p.isEmpty()?new Preferencia(false,true,LocalTime.of(22,0),LocalTime.of(7,0),zona):p.getFirst();
	}
	private static String preferenciaDe(String tipo){return TipoAviso.valueOf(tipo).preferencia();}
	/** Instante em que o silêncio termina, ou {@code null} se agora está fora dele. */
	static Instant fimDoSilencio(Preferencia p,Instant agora){
		var local=agora.atZone(p.fuso());var hora=local.toLocalTime();
		boolean viraMeiaNoite=p.inicio().isAfter(p.fim());
		boolean dentro=viraMeiaNoite?(!hora.isBefore(p.inicio())||hora.isBefore(p.fim())):(!hora.isBefore(p.inicio())&&hora.isBefore(p.fim()));
		if(!dentro)return null;
		var data=local.toLocalDate();
		if(viraMeiaNoite&&!hora.isBefore(p.inicio()))data=data.plusDays(1);
		return data.atTime(p.fim()).atZone(p.fuso()).toInstant();
	}
}
