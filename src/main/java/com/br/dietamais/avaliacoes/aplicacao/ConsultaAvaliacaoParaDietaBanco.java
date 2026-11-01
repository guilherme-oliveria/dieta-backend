package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.avaliacoes.infra.AvaliacaoStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConsultaAvaliacaoParaDietaBanco implements ConsultaAvaliacaoParaDieta {
	private static final BigDecimal CEM=BigDecimal.valueOf(100),DOIS=BigDecimal.valueOf(2),QUATRO=BigDecimal.valueOf(4);
	private final AvaliacaoStore store;
	public ConsultaAvaliacaoParaDietaBanco(AvaliacaoStore store){this.store=store;}
	@Override @Transactional(readOnly=true)
	public Optional<ReferenciaAvaliacaoDietaResposta> maisRecente(UUID nutricionista,UUID paciente){
		var a=store.maisRecenteDoPaciente(paciente,nutricionista);
		if(a==null)return Optional.empty();
		var r=a.energiaResultado();var e=a.energia();
		var pesoRef=r!=null&&r.pesoReferenciaKg()!=null?r.pesoReferenciaKg():a.pesoKg();
		return Optional.of(new ReferenciaAvaliacaoDietaResposta(a.id(),a.dataHora(),a.pesoKg(),pesoRef,metas(r),e!=null&&Boolean.TRUE.equals(e.gestante()),e!=null&&e.lactacao()!=null));
	}
	/** Meio da faixa em gramas de cada macro; o lipídio fecha o que sobra (planilha: 100 − %gli − %ptn). */
	static MetasDieta metas(EnergiaResultado r){
		if(r==null||r.vetKcal()==null||r.vetKcal().signum()<=0||r.carboidrato()==null||r.proteina()==null)return null;
		var carbo=pct(r.carboidrato(),r.vetKcal());var ptn=pct(r.proteina(),r.vetKcal());
		if(carbo==null||ptn==null)return null;
		return new MetasDieta(r.vetKcal(),carbo,ptn,CEM.subtract(carbo).subtract(ptn).max(BigDecimal.ZERO));
	}
	private static BigDecimal pct(MetaMacro m,BigDecimal vet){
		if(m.gramasMinimo()==null)return null;
		var gramas=m.gramasMaximo()==null?m.gramasMinimo():m.gramasMinimo().add(m.gramasMaximo()).divide(DOIS);
		return gramas.multiply(QUATRO).multiply(CEM).divide(vet,2,RoundingMode.HALF_UP);
	}
}
