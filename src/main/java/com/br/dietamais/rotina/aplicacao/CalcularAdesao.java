package com.br.dietamais.rotina.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.AdesaoDosPacientes;
import com.br.dietamais.pacientes.api.ConsultarFichaParaLista;
import com.br.dietamais.rotina.api.AdesaoResposta;
import com.br.dietamais.rotina.api.JanelaAdesao;
import com.br.dietamais.rotina.infra.AdesaoStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN-ROT-02: adesão = refeições FEITO ou TROCADO sobre as esperadas (refeições da dieta publicada × dias desde a primeira publicação, até a janela). */
@Service
public class CalcularAdesao implements AdesaoDosPacientes {
	private final AdesaoStore store;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;private final AuditoriaClinica auditoria;private final Clock relogio;
	public CalcularAdesao(AdesaoStore store,ConsultarFichaParaLista fichas,UsuarioAtual usuario,AuditoriaClinica auditoria,Clock relogio){this.store=store;this.fichas=fichas;this.usuario=usuario;this.auditoria=auditoria;this.relogio=relogio;}

	@Transactional(readOnly=true)
	public AdesaoResposta paciente(UUID paciente){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();
		var ficha=fichas.consultar(paciente,false);
		var fuso=ZoneId.of(contexto.usuario().fuso());
		var v=List.of(ficha.vinculoId());
		var resposta=new AdesaoResposta(ficha.pacienteId(),janela(v,fuso,7).get(ficha.vinculoId()),janela(v,fuso,30).get(ficha.vinculoId()));
		auditoria.leitura(new ReferenciaClinica("checkin",ficha.vinculoId(),nutri,ficha.pacienteId(),contexto.organizacaoId()));
		return resposta;
	}
	@Override @Transactional(readOnly=true)
	public Map<UUID,BigDecimal> adesao7d(Collection<UUID> vinculos,ZoneId fuso){
		var r=new HashMap<UUID,BigDecimal>();
		janela(vinculos,fuso,7).forEach((id,j)->{if(j.percentual()!=null)r.put(id,j.percentual());});
		return r;
	}
	/** Janela por vínculo; vínculo sem dieta publicada ainda recebe janela zerada com percentual nulo. */
	private Map<UUID,JanelaAdesao> janela(Collection<UUID> vinculos,ZoneId fuso,int dias){
		var hoje=LocalDate.now(relogio.withZone(fuso));var inicioJanela=hoje.minusDays(dias-1L);
		var bases=store.bases(vinculos);var marcas=store.agrupar(store.marcas(vinculos,inicioJanela,hoje));
		var r=new HashMap<UUID,JanelaAdesao>();
		for(var id:vinculos)r.put(id,new JanelaAdesao(0,0,0,0,0,null));
		for(var b:bases){
			var primeira=b.primeiraPublicacao().atZone(fuso).toLocalDate();
			var inicio=primeira.isAfter(inicioJanela)?primeira:inicioJanela;
			int n=b.refeicoesPorDia()==null?0:b.refeicoesPorDia();
			int diasContados=(int)java.time.temporal.ChronoUnit.DAYS.between(inicio,hoje)+1;
			int esperadas=n*Math.max(diasContados,0);
			int feitas=0,trocadas=0,puladas=0;
			for(var m:marcas.getOrDefault(b.vinculoId(),List.of()))switch(m.status()){case "FEITO"->feitas++;case "TROCADO"->trocadas++;default->puladas++;}
			BigDecimal pct=esperadas==0?null:BigDecimal.valueOf(Math.min(feitas+trocadas,esperadas)*100L).divide(BigDecimal.valueOf(esperadas),1,RoundingMode.HALF_UP);
			r.put(b.vinculoId(),new JanelaAdesao(Math.max(diasContados,0),esperadas,feitas,trocadas,puladas,pct));
		}
		return r;
	}
}
