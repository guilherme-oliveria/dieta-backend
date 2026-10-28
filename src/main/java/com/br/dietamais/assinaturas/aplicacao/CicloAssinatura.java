package com.br.dietamais.assinaturas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.notificacoes.api.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida da assinatura (BE-27, RN-ASS-02/04/07/08). Roda de hora em hora e é idempotente: cada mudança tem chave de dedup própria.
 * O paciente não é afetado (RN-ASS-05); nenhum vínculo é encerrado por downgrade (RN-ASS-08).
 */
@Service
public class CicloAssinatura {
	public record Resultado(int expiradas,int atrasadas,int avisos) { }
	private final JdbcTemplate jdbc;private final Clock relogio;private final ConsultaNutricionistas nutris;private final RegistrarAviso avisos;
	private final Duration carencia;private final Duration tolerancia;private final Duration antecedenciaTeste;
	public CicloAssinatura(JdbcTemplate jdbc,Clock relogio,ConsultaNutricionistas nutris,RegistrarAviso avisos,
			@Value("${dieta.assinatura.carencia:P7D}") Duration carencia,
			@Value("${dieta.assinatura.tolerancia-webhook:P3D}") Duration tolerancia,
			@Value("${dieta.assinatura.aviso-fim-teste:P3D}") Duration antecedenciaTeste) {
		this.jdbc=jdbc;this.relogio=relogio;this.nutris=nutris;this.avisos=avisos;this.carencia=carencia;this.tolerancia=tolerancia;this.antecedenciaTeste=antecedenciaTeste;
	}

	@Scheduled(fixedDelayString="${dieta.assinatura.intervalo-ciclo:PT1H}",initialDelayString="${dieta.assinatura.intervalo-ciclo:PT1H}")
	void agendado() {
		try { var r=executar();LoggerFactory.getLogger(CicloAssinatura.class).info("Ciclo de assinaturas: expiradas={} atrasadas={} avisos={}",r.expiradas(),r.atrasadas(),r.avisos()); }
		catch (RuntimeException erro) { LoggerFactory.getLogger(CicloAssinatura.class).warn("Ciclo de assinaturas falhou: {}",erro.getClass().getSimpleName()); }
	}

	@Transactional
	public Resultado executar() {
		var agora=relogio.instant();int avisados=0;
		// RN-ASS-02: o teste acabou
		var expiradas=jdbc.query("update assinatura set status='EXPIRADA' where status='TESTE' and fim_periodo_em<=? returning id,organizacao_id",
			(rs,n)->new UUID[]{rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)},Timestamp.from(agora));
		for(var e:expiradas)avisados+=avisar(e[1],"EXPIRADA:"+e[0]);
		// o teste está acabando
		for(var e:jdbc.query("select id,organizacao_id from assinatura where status='TESTE' and fim_periodo_em>? and fim_periodo_em<=?",
				(rs,n)->new UUID[]{rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)},Timestamp.from(agora),Timestamp.from(agora.plus(antecedenciaTeste))))
			avisados+=avisar(e[1],"TESTE_FIM:"+e[0]);
		// o gateway não avisou do atraso (webhook perdido): depois da tolerância, entra em atraso
		var atrasadas=jdbc.query("update assinatura set status='EM_ATRASO' where status='ATIVA' and fim_periodo_em<=? returning id,organizacao_id,fim_periodo_em",
			(rs,n)->new Object[]{rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getTimestamp(3).toInstant()},Timestamp.from(agora.minus(tolerancia)));
		for(var a:atrasadas)avisados+=avisar((UUID)a[1],"ATRASO:"+a[0]+":"+((Instant)a[2]).getEpochSecond());
		// passou a carência: a nutri fica só leitura (RN-ASS-04); avisa uma vez por vencimento
		for(var b:jdbc.query("select id,organizacao_id,fim_periodo_em from assinatura where status='EM_ATRASO' and fim_periodo_em<=?",
				(rs,n)->new Object[]{rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getTimestamp(3).toInstant()},Timestamp.from(agora.minus(carencia))))
			avisados+=avisar((UUID)b[1],"BLOQUEIO:"+b[0]+":"+((Instant)b[2]).getEpochSecond());
		return new Resultado(expiradas.size(),atrasadas.size(),avisados);
	}

	private int avisar(UUID organizacao,String chave) {
		int n=0;
		for(var nutri:nutris.daOrganizacao(organizacao)){
			var antes=jdbc.queryForObject("select count(*) from aviso a join nutricionista n on n.usuario_id=a.usuario_id where n.id=? and a.chave_dedup=?",Integer.class,nutri,chave);
			avisos.registrar(new NovoAviso(Perfil.NUTRICIONISTA,nutri,TipoAviso.ASSINATURA,"/assinatura",chave));
			if(antes!=null&&antes==0)n++;
		}
		return n;
	}
}
