package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.infra.AvaliacaoStore;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.rotina.api.PesosEmCasa;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * Séries de evolução: peso, IMC, composição, perímetros e peso em casa. A nutri vê todas as avaliações do vínculo e o peso em casa só com vínculo ativo
 * (como no sync); o paciente vê só as avaliações compartilhadas (RN-AVA-05) e os próprios pesos.
 */
@Service
public class ConsultarEvolucao {
	/** Teto por série, para a resposta não crescer sem limite; vale os pontos mais recentes. */
	static final int LIMITE_PONTOS=500;
	private static final Map<MedidaEvolucao,Function<AvaliacaoResposta,BigDecimal>> EXTRATORES=extratores();
	private final AvaliacaoStore avaliacoes;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;private final AuditoriaClinica auditoria;private final PesosEmCasa pesos;
	public ConsultarEvolucao(AvaliacaoStore avaliacoes,ConsultarFichaParaLista fichas,UsuarioAtual usuario,AuditoriaClinica auditoria,PesosEmCasa pesos){
		this.avaliacoes=avaliacoes;this.fichas=fichas;this.usuario=usuario;this.auditoria=auditoria;this.pesos=pesos;
	}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public EvolucaoResposta daNutri(UUID paciente){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var ficha=fichas.consultar(paciente,false);
		var itens=avaliacoes.porVinculo(ficha.vinculoId(),nutri);
		var emCasa=ficha.status()==StatusVinculo.ATIVO?pesos.doPaciente(ficha.pacienteId(),LIMITE_PONTOS):List.<PesosEmCasa.Ponto>of();
		auditoria.leitura(new ReferenciaClinica("avaliacao",ficha.vinculoId(),nutri,ficha.pacienteId(),contexto.organizacaoId()));
		return new EvolucaoResposta(ficha.pacienteId(),series(itens,emCasa));
	}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public EvolucaoResposta minha(){
		var paciente=usuario.obter().exigirPaciente();
		return new EvolucaoResposta(paciente,series(avaliacoes.compartilhadasDoPaciente(paciente),pesos.doPaciente(paciente,LIMITE_PONTOS)));
	}

	static List<SerieEvolucao> series(List<AvaliacaoResposta> avaliacoes,List<PesosEmCasa.Ponto> emCasa){
		var ordenadas=avaliacoes.stream().sorted(Comparator.comparing(AvaliacaoResposta::dataHora).thenComparing(AvaliacaoResposta::criadoEm)).toList();
		var series=new ArrayList<SerieEvolucao>();
		for(var medida:MedidaEvolucao.values()){
			List<PontoEvolucao> pontos;
			if(medida==MedidaEvolucao.PESO_EM_CASA)pontos=emCasa.stream().map(p->new PontoEvolucao(p.registradoEm(),p.pesoKg())).toList();
			else{
				var extrair=EXTRATORES.get(medida);
				pontos=ordenadas.stream().map(a->{var v=extrair.apply(a);return v==null?null:new PontoEvolucao(a.dataHora(),v);}).filter(Objects::nonNull).toList();
			}
			if(pontos.size()>LIMITE_PONTOS)pontos=pontos.subList(pontos.size()-LIMITE_PONTOS,pontos.size());
			if(!pontos.isEmpty())series.add(new SerieEvolucao(medida,medida.unidade(),pontos));
		}
		return series;
	}

	private static Map<MedidaEvolucao,Function<AvaliacaoResposta,BigDecimal>> extratores(){
		var m=new EnumMap<MedidaEvolucao,Function<AvaliacaoResposta,BigDecimal>>(MedidaEvolucao.class);
		m.put(MedidaEvolucao.PESO,AvaliacaoResposta::pesoKg);m.put(MedidaEvolucao.IMC,AvaliacaoResposta::imc);
		m.put(MedidaEvolucao.PERCENTUAL_GORDURA,a->a.composicao().percentualGordura());m.put(MedidaEvolucao.PESO_GORDO,a->a.composicao().pesoGordoKg());m.put(MedidaEvolucao.PESO_MAGRO,a->a.composicao().pesoMagroKg());
		m.put(MedidaEvolucao.PERIMETRO_PESCOCO,a->a.perimetros().pescoco());m.put(MedidaEvolucao.PERIMETRO_TORAX,a->a.perimetros().torax());m.put(MedidaEvolucao.PERIMETRO_CINTURA,a->a.perimetros().cintura());
		m.put(MedidaEvolucao.PERIMETRO_ABDOMEN,a->a.perimetros().abdomen());m.put(MedidaEvolucao.PERIMETRO_QUADRIL,a->a.perimetros().quadril());m.put(MedidaEvolucao.PERIMETRO_BRACO,a->a.perimetros().braco());
		m.put(MedidaEvolucao.PERIMETRO_ANTEBRACO,a->a.perimetros().antebraco());m.put(MedidaEvolucao.PERIMETRO_COXA,a->a.perimetros().coxa());m.put(MedidaEvolucao.PERIMETRO_PANTURRILHA,a->a.perimetros().panturrilha());
		m.put(MedidaEvolucao.SOMATORIO_DOBRAS,a->a.resultados()==null?null:a.resultados().somatorioDobrasMm());
		m.put(MedidaEvolucao.DOBRA_TRICEPS,a->a.dobras().triceps());m.put(MedidaEvolucao.DOBRA_SUBESCAPULAR,a->a.dobras().subescapular());m.put(MedidaEvolucao.DOBRA_PEITORAL,a->a.dobras().peitoral());
		m.put(MedidaEvolucao.DOBRA_AXILAR_MEDIA,a->a.dobras().axilarMedia());m.put(MedidaEvolucao.DOBRA_SUPRAILIACA,a->a.dobras().suprailiaca());m.put(MedidaEvolucao.DOBRA_ABDOMINAL,a->a.dobras().abdominal());m.put(MedidaEvolucao.DOBRA_COXA,a->a.dobras().coxa());
		return m;
	}
}
