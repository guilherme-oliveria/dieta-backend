package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.*;
import com.br.dietamais.pacientes.api.FichaParaLista;
import com.br.dietamais.pacientes.api.Sexo;
import com.br.dietamais.referencias.api.ConsultaReferencias;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Orquestra os cálculos derivados da avaliação (RN-CAL-01): idade na data, faixa etária (RN-CAL-02), protocolo (RN-CAL-03)
 * e a calculadora do adulto. Sem sexo, nascimento ou faixa 18+, devolve só o que dá para saber, com o motivo (RN-CAL-04).
 */
@Component
public class CalculoAvaliacao {
	/** Dia da avaliação para a idade: o fuso do Brasil, onde a nutri atende. */
	static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
	public static final ProtocoloDobras DOBRAS_PADRAO=ProtocoloDobras.SETE;
	public static final ProtocoloMassa MASSA_PADRAO=ProtocoloMassa.MAGRO;
	private final CalculadoraAntropometriaAdulto adulto;private final ConsultaReferencias referencias;
	public CalculoAvaliacao(ConsultaReferencias referencias){this.referencias=referencias;this.adulto=new CalculadoraAntropometriaAdulto(referencias);}

	public record Calculado(FaixaEtaria faixaEtaria,ProtocoloAvaliacao protocolo,ResultadosAvaliacao resultados) {}

	public static FaixaEtaria faixa(int idadeAnos){return idadeAnos<10?FaixaEtaria.CRIANCA:idadeAnos<18?FaixaEtaria.ADOLESCENTE:idadeAnos<60?FaixaEtaria.ADULTO:FaixaEtaria.IDOSO;}

	/** {@code protocoloPedido} nulo usa o padrão; a versão gravada é a vigente, nunca a que o cliente mandou. */
	public Calculado calcular(SalvarAvaliacaoRequisicao r,FichaParaLista ficha,ProtocoloAvaliacao protocoloPedido,BigDecimal imc){
		var protocolo=new ProtocoloAvaliacao(protocoloPedido==null?DOBRAS_PADRAO:protocoloPedido.dobras(),protocoloPedido==null?MASSA_PADRAO:protocoloPedido.massa(),referencias.versaoVigente());
		if(ficha.dataNascimento()==null)return semDados(protocolo,null,"FALTA_NASCIMENTO");
		var dia=r.dataHora().atZone(FUSO).toLocalDate();
		int idade=CalculadoraAntropometriaAdulto.idadeEmAnos(ChronoUnit.DAYS.between(ficha.dataNascimento(),dia));
		var faixa=faixa(Math.max(idade,0));
		if(idade<0)return semDados(protocolo,null,"FALTA_NASCIMENTO");
		if(faixa==FaixaEtaria.CRIANCA||faixa==FaixaEtaria.ADOLESCENTE)return new Calculado(faixa,protocolo,new ResultadosAvaliacao(idade,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,Map.of("resultados","FAIXA_ETARIA_NAO_SUPORTADA")));
		String sexo=ficha.sexo()==Sexo.MASCULINO?"masculino":ficha.sexo()==Sexo.FEMININO?"feminino":null;
		if(sexo==null)return new Calculado(faixa,protocolo,new ResultadosAvaliacao(idade,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,Map.of("resultados","FALTA_SEXO")));
		var d=r.dobras()==null?DobrasAvaliacao.VAZIO:r.dobras();var p=r.perimetros()==null?PerimetrosAvaliacao.VAZIO:r.perimetros();
		var c=r.complementares();
		var entrada=new EntradaAvaliacaoAdulto(sexo,idade,r.pesoKg(),r.estaturaM(),protocolo.dobras(),protocolo.massa(),d.triceps(),d.peitoral(),d.axilarMedia(),d.subescapular(),
			d.abdominal(),d.suprailiaca(),d.coxa(),p.cintura(),c==null?null:c.punhoCm(),c==null?null:c.diametroPunhoCm(),c==null?null:c.diametroJoelhoCm(),c==null?null:c.percentualGorduraDesejado());
		return new Calculado(faixa,protocolo,adulto.avaliar(entrada).resultados());
	}
	private Calculado semDados(ProtocoloAvaliacao protocolo,Integer idade,String motivo){
		return new Calculado(null,protocolo,new ResultadosAvaliacao(idade,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,Map.of("resultados",motivo)));
	}
}
