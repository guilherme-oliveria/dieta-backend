package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.*;
import com.br.dietamais.pacientes.api.FichaParaLista;
import com.br.dietamais.pacientes.api.Sexo;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Orquestra os cálculos derivados da avaliação (RN-CAL-01): idade na data, faixa etária (RN-CAL-02), protocolo (RN-CAL-03)
 * e as calculadoras adequadas à faixa. Sem sexo ou nascimento, devolve só o que dá para saber, com o motivo (RN-CAL-04).
 */
@Component
public class CalculoAvaliacao {
	/** Dia da avaliação para a idade: o fuso do Brasil, onde a nutri atende. */
	static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
	public static final ProtocoloDobras DOBRAS_PADRAO=ProtocoloDobras.SETE;
	public static final ProtocoloMassa MASSA_PADRAO=ProtocoloMassa.MAGRO;
	private final CalculadoraAntropometriaAdulto adulto;private final ConsultaReferencias referencias;private final CalculoEnergiaAvaliacao energia;private final CalculoAvaliacaoInfantil infantil;
	@org.springframework.beans.factory.annotation.Autowired
	public CalculoAvaliacao(ConsultaReferencias referencias,ConsultaAtividadesMet atividades,CalculoAvaliacaoInfantil infantil){this.referencias=referencias;this.adulto=new CalculadoraAntropometriaAdulto(referencias);this.energia=new CalculoEnergiaAvaliacao(atividades);this.infantil=infantil;}
	/** Compatibilidade para consumidores que só fornecem as referências adultas. */
	public CalculoAvaliacao(ConsultaReferencias referencias,ConsultaAtividadesMet atividades){this(referencias,atividades,new CalculoAvaliacaoInfantil(new CalculoCurvasInfantis((i,s,m)->Optional.empty()),new CalculoFrisanchoAvaliacao((i,s,a)->Optional.empty())));}

	public record Calculado(FaixaEtaria faixaEtaria,ProtocoloAvaliacao protocolo,ResultadosAvaliacao resultados,EnergiaResultado energia,String sexo,ResultadosInfantisAvaliacao infantil) {
	public Calculado(FaixaEtaria faixaEtaria,ProtocoloAvaliacao protocolo,ResultadosAvaliacao resultados,EnergiaResultado energia,String sexo){this(faixaEtaria,protocolo,resultados,energia,sexo,null);}
	public Calculado(FaixaEtaria faixaEtaria,ProtocoloAvaliacao protocolo,ResultadosAvaliacao resultados,EnergiaResultado energia){this(faixaEtaria,protocolo,resultados,energia,null);}
}

	public static FaixaEtaria faixa(int idadeAnos){return idadeAnos<10?FaixaEtaria.CRIANCA:idadeAnos<18?FaixaEtaria.ADOLESCENTE:idadeAnos<60?FaixaEtaria.ADULTO:FaixaEtaria.IDOSO;}

	/** {@code protocoloPedido} nulo usa o padrão; a versão gravada é a vigente, nunca a que o cliente mandou. */
	public Calculado calcular(SalvarAvaliacaoRequisicao r,FichaParaLista ficha,ProtocoloAvaliacao protocoloPedido,BigDecimal imc){
		var protocolo=new ProtocoloAvaliacao(protocoloPedido==null?DOBRAS_PADRAO:protocoloPedido.dobras(),protocoloPedido==null?MASSA_PADRAO:protocoloPedido.massa(),referencias.versaoVigente());
		if(ficha.dataNascimento()==null)return semDados(r,protocolo,null,"FALTA_NASCIMENTO");
		var dia=r.dataHora().atZone(FUSO).toLocalDate();
		if(dia.isBefore(ficha.dataNascimento()))return semDados(r,protocolo,null,"FALTA_NASCIMENTO");
		int idade=Math.toIntExact(ChronoUnit.YEARS.between(ficha.dataNascimento(),dia));
		var faixa=faixa(Math.max(idade,0));
		if(idade<0)return semDados(r,protocolo,null,"FALTA_NASCIMENTO");
		if(faixa==FaixaEtaria.CRIANCA||faixa==FaixaEtaria.ADOLESCENTE)return new Calculado(faixa,protocolo,new ResultadosAvaliacao(idade,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,Map.of("classificacaoImc","USE_CURVA_IMC_IDADE")),semEnergia(r,"USE_ENERGIA_INFANTIL"),sexoTexto(ficha),infantil.calcular(r,ficha));
		String sexo=sexoTexto(ficha);
		if(sexo==null)return new Calculado(faixa,protocolo,new ResultadosAvaliacao(idade,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,Map.of("resultados","FALTA_SEXO")),semEnergia(r,"FALTA_SEXO"),null);
		var d=r.dobras()==null?DobrasAvaliacao.VAZIO:r.dobras();var p=r.perimetros()==null?PerimetrosAvaliacao.VAZIO:r.perimetros();
		var c=r.complementares();
		var entrada=new EntradaAvaliacaoAdulto(sexo,idade,r.pesoKg(),r.estaturaM(),protocolo.dobras(),protocolo.massa(),d.triceps(),d.peitoral(),d.axilarMedia(),d.subescapular(),
			d.abdominal(),d.suprailiaca(),d.coxa(),p.cintura(),c==null?null:c.punhoCm(),c==null?null:c.diametroPunhoCm(),c==null?null:c.diametroJoelhoCm(),c==null?null:c.percentualGorduraDesejado());
		var resultados=adulto.avaliar(entrada).resultados();
		return new Calculado(faixa,protocolo,resultados,CalculoEnergiaAvaliacao.pedida(r)?energia.calcular(r,sexo,idade,resultados):null,sexo);
	}
	private static String sexoTexto(FichaParaLista ficha){return ficha.sexo()==Sexo.MASCULINO?"masculino":ficha.sexo()==Sexo.FEMININO?"feminino":null;}
	private EnergiaResultado semEnergia(SalvarAvaliacaoRequisicao r,String motivo){return CalculoEnergiaAvaliacao.pedida(r)?energia.semDados(r,motivo):null;}
	private Calculado semDados(SalvarAvaliacaoRequisicao r,ProtocoloAvaliacao protocolo,Integer idade,String motivo){
		return new Calculado(null,protocolo,new ResultadosAvaliacao(idade,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,Map.of("resultados",motivo)),semEnergia(r,motivo));
	}
}
