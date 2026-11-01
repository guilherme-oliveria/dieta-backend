package com.br.dietamais.rotina.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.rotina.api.*;
import com.br.dietamais.rotina.infra.RotinaStore;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Escritas só de inclusão do paciente (RN-ROT-01..03): check-in, água e peso. Repetir o mesmo id devolve o registro já gravado. */
@Service
public class RegistrarRotina {
	static final int DIAS_RETROATIVOS=3;
	private final GeradorIds ids;private final RotinaStore rotina;private final ConsultaSnapshotsPaciente snapshots;private final UsuarioAtual usuario;private final Clock relogio;
	public RegistrarRotina(GeradorIds ids,RotinaStore rotina,ConsultaSnapshotsPaciente snapshots,UsuarioAtual usuario,Clock relogio){this.ids=ids;this.rotina=rotina;this.snapshots=snapshots;this.usuario=usuario;this.relogio=relogio;}

	@Transactional
	public CheckinResposta checkin(CheckinRequisicao r){
		var contexto=usuario.obter();var paciente=contexto.exigirPaciente();
		UUID id=id(r.id());
		var s=snapshots.obter(r.dietaId(),paciente).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		if(!s.vinculoAtivo())throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		var refeicao=s.snapshot().refeicoes().stream().filter(x->x.id().equals(r.refeicaoId())).findFirst().orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		var hoje=LocalDate.now(relogio.withZone(ZoneId.of(contexto.usuario().fuso())));
		if(r.data().isAfter(hoje)||r.data().isBefore(hoje.minusDays(DIAS_RETROATIVOS))||r.registradoEm().isAfter(relogio.instant().plusSeconds(300)))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ROT-01");
		var trocas=r.trocas()==null?List.<TrocaCheckin>of():r.trocas();
		if((r.status()==StatusCheckin.TROCADO)==trocas.isEmpty())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SUB-03");
		var lista=new HashMap<UUID,UUID>();s.snapshot().listaSubstituicao().alimentos().forEach(a->lista.put(a.id(),a.grupoId()));
		var itensTrocados=new HashSet<UUID>();
		for(var t:trocas){
			var item=refeicao.itens().stream().filter(i->i.id().equals(t.itemId())).findFirst().orElseThrow(()->new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SUB-01"));
			if(!itensTrocados.add(t.itemId())||item.grupoId()==null||!item.grupoId().equals(lista.get(t.alimentoId())))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SUB-01");
		}
		var vinculo=s.snapshot().vinculoId();
		if(rotina.vinculoAtivo(vinculo,paciente)==null)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		if(!rotina.inserirCheckin(id,paciente,vinculo,s.snapshot().nutricionista().id(),r,trocas)&&rotina.checkin(id,paciente)==null)throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		return rotina.checkin(id,paciente);
	}
	@Transactional
	public AguaResposta agua(AguaRequisicao r){
		var paciente=usuario.obter().exigirPaciente();UUID id=id(r.id());janela(r.registradoEm());
		if(!rotina.inserirAgua(id,paciente,r)&&rotina.agua(id,paciente)==null)throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		return rotina.agua(id,paciente);
	}
	@Transactional
	public PesoResposta peso(PesoRequisicao r){
		var paciente=usuario.obter().exigirPaciente();UUID id=id(r.id());janela(r.registradoEm());
		BigDecimal kg=r.pesoKg();
		if(kg.compareTo(new BigDecimal("20"))<0||kg.compareTo(new BigDecimal("400"))>0)throw new ExcecaoNegocio(CodigoErro.VALOR_FORA_DO_INTERVALO,"RN-AVA-03");
		if(kg.stripTrailingZeros().scale()>2)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ROT-03");
		if(!rotina.inserirPeso(id,paciente,kg,r.registradoEm())&&rotina.peso(id,paciente)==null)throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		return rotina.peso(id,paciente);
	}
	private void janela(Instant quando){
		var agora=relogio.instant();
		if(quando.isAfter(agora.plusSeconds(300))||quando.isBefore(agora.minus(Duration.ofDays(DIAS_RETROATIVOS+1))))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ROT-03");
	}
	private UUID id(UUID id){
		if(id==null)return ids.novo();
		if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);return id;
	}
}
