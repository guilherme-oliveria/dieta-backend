package com.br.dietamais.rotina.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.rotina.api.RotinaResposta;
import com.br.dietamais.rotina.infra.RotinaStore;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura da rotina do próprio paciente num período (BE-38). O dono vem de {@link UsuarioAtual}, nunca da requisição. */
@Service
public class ConsultarRotina {
	static final int DIAS_MAXIMOS=31;
	private final RotinaStore rotina;private final UsuarioAtual usuario;
	public ConsultarRotina(RotinaStore rotina,UsuarioAtual usuario){this.rotina=rotina;this.usuario=usuario;}

	@Transactional(readOnly=true)
	public RotinaResposta periodo(LocalDate de,LocalDate ate){
		var contexto=usuario.obter();var paciente=contexto.exigirPaciente();
		if(de==null||ate==null||ate.isBefore(de)||ChronoUnit.DAYS.between(de,ate)+1>DIAS_MAXIMOS)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		var fuso=ZoneId.of(contexto.usuario().fuso());
		var inicio=de.atStartOfDay(fuso).toInstant();var fim=ate.plusDays(1).atStartOfDay(fuso).toInstant();
		return new RotinaResposta(rotina.checkinsDoPeriodo(paciente,de,ate),rotina.aguaDoPeriodo(paciente,inicio,fim),rotina.pesosDoPeriodo(paciente,inicio,fim));
	}
}
