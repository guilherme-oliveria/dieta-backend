package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.infra.ConsultaStore;
import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.notificacoes.api.*;
import java.time.*;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * RN-AGE-02 / RN-NOT-01: aviso 24 h antes da consulta, para o paciente e para a nutri. O texto não traz horário nem local
 * (RN-NOT-02). A chave de dedup inclui o início: remarcar gera um aviso novo. O lembrete por e-mail ainda não existe.
 */
@Service
public class LembretesConsulta {
	static final Duration ANTECEDENCIA=Duration.ofHours(24);
	private final ConsultaStore consultas;private final RegistrarAviso avisos;private final Clock relogio;
	public LembretesConsulta(ConsultaStore consultas,RegistrarAviso avisos,Clock relogio){this.consultas=consultas;this.avisos=avisos;this.relogio=relogio;}

	@Scheduled(fixedDelayString="${dieta.avisos.intervalo-consultas:PT15M}",initialDelayString="${dieta.avisos.intervalo-consultas:PT15M}")
	public void executarJob(){
		try { executar(); }
		catch (RuntimeException erro) { LoggerFactory.getLogger(LembretesConsulta.class).warn("Falha técnica no job de lembrete de consulta."); }
	}
	public int executar(){
		var agora=relogio.instant();var lista=consultas.aComecar(agora,agora.plus(ANTECEDENCIA));
		for(var c:lista){
			var chave="consulta:"+c.id()+":"+c.inicio().getEpochSecond();
			avisos.registrar(new NovoAviso(Perfil.PACIENTE,c.pacienteId(),TipoAviso.CONSULTA_AMANHA,"/agenda",chave));
			avisos.registrar(new NovoAviso(Perfil.NUTRICIONISTA,c.nutricionistaId(),TipoAviso.CONSULTA_AMANHA,"/agenda",chave));
		}
		return lista.size();
	}
}
