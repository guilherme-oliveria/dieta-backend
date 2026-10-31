package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.energia.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.referencias.api.ConsultaAtividadesMet;
import java.math.BigDecimal;
import java.util.*;

/**
 * Gasto energético e metas da avaliação (BE-43b, RN-CAL-01): escolhe a fórmula pelo {@link MetodoGet}, aplica o objetivo e os protocolos
 * da nutri e devolve o que dá para saber, com o motivo do que falta (RN-CAL-04). Sem escolha da nutri vale FAO 2004, faixa DRI de
 * carboidrato e proteína e lipídio mínimo de 20% [SUPOSIÇÃO S-energia: padrões configuráveis na próxima revisão].
 */
class CalculoEnergiaAvaliacao {
	static final MetodoGet METODO_PADRAO=MetodoGet.FAO_2004;
	private final ConsultaAtividadesMet atividades;
	CalculoEnergiaAvaliacao(ConsultaAtividadesMet atividades){this.atividades=atividades;}

	/** Só calcula quando a nutri escolheu algo ou informou o nível de atividade; avaliação antiga continua sem o bloco. */
	static boolean pedida(SalvarAvaliacaoRequisicao r){
		return r.energia()!=null||(r.complementares()!=null&&r.complementares().nivelAtividade()!=null);
	}

	EnergiaResultado semDados(SalvarAvaliacaoRequisicao r,String motivo){
		return new EnergiaResultado(metodo(r.energia()),null,null,null,null,null,null,null,null,null,Map.of("energia",motivo));
	}

	EnergiaResultado calcular(SalvarAvaliacaoRequisicao r,String sexo,int idade,ResultadosAvaliacao resultados){
		var e=r.energia();var c=r.complementares();var metodo=metodo(e);var motivos=new LinkedHashMap<String,String>();
		boolean mulher="feminino".equals(sexo);
		boolean lactante=mulher&&e!=null&&e.lactacao()!=null,gestante=mulher&&e!=null&&Boolean.TRUE.equals(e.gestante());
		var nivel=c==null?null:c.nivelAtividade();var peso=r.pesoKg();var estatura=r.estaturaM();
		BigDecimal geb=null,get=null;
		switch(metodo){
			case FAO_1985->{
				geb=CalculadoraEnergia.gebFao1985(sexo,idade,peso);
				if(nivel==null)motivos.put("get","FALTA_NIVEL_ATIVIDADE");else get=CalculadoraEnergia.getFao1985(geb,sexo,nivel);
			}
			case FAO_2004->{
				geb=CalculadoraEnergia.gebFao2004(sexo,idade,peso);
				if(nivel==null)motivos.put("get","FALTA_NIVEL_ATIVIDADE");else get=CalculadoraEnergia.getFao2004(geb,nivel,lactante);
			}
			case IOM_2005->{
				var classe=resultados==null?null:resultados.classificacaoImc();
				geb=CalculadoraEnergia.gebIom2002(sexo,idade,peso,estatura,classe==ClasseImc.SP||classe==ClasseImc.OB_I||classe==ClasseImc.OB_II||classe==ClasseImc.OB_III);
				if(nivel==null)motivos.put("get","FALTA_NIVEL_ATIVIDADE");else get=CalculadoraEnergia.getIom2005(geb,nivel);
			}
			case RDI_2005->{
				if(nivel==null)motivos.put("get","FALTA_NIVEL_ATIVIDADE");
				else get=CalculadoraEnergia.getRdi2005(sexo,idade,peso,estatura,nivel,lactante?e.lactacao():null);
			}
			case MET->{
				var magra=massaMagra(r,resultados);
				if(e==null||e.rotinaMet()==null)motivos.put("get","FALTA_ROTINA_MET");
				else if(magra==null)motivos.put("get","FALTA_MASSA_MAGRA");
				else{
					var met=met(e.rotinaMet(),sexo,peso,magra);
					geb=met.gebDia();get=met.get();
				}
			}
		}
		BigDecimal vet=null,kcalKg=null;MetaMacro carbo=null,ptn=null,lip=null;LipidiosDislipidemico dislip=null;BigDecimal colesterol=null;
		if(get==null)motivos.put("vet","FALTA_GET");
		else{
			vet=CalculadoraEnergia.vet(get,c==null?null:c.objetivo(),c==null?null:c.objetivoKgPorMes());
			kcalKg=CalculadoraEnergia.kcalPorKg(vet,peso);
			carbo=CalculadoraEnergia.carboidrato(e==null||e.carboidrato()==null?ProtocoloCarboidrato.FAIXA_DRI:e.carboidrato(),vet,peso,gestante,lactante);
			ptn=CalculadoraEnergia.proteina(e==null||e.proteina()==null?ProtocoloProteina.FAIXA_DRI:e.proteina(),vet,peso,sexo,idade,gestante,lactante);
			lip=CalculadoraEnergia.lipidio(e==null||e.lipidio()==null?ProtocoloLipidio.MINIMO_20:e.lipidio(),vet);
			if(e!=null&&Boolean.TRUE.equals(e.dislipidemico())){
				dislip=CalculadoraEnergia.lipidiosDislipidemico(vet);colesterol=CalculadoraEnergia.colesterolMaximoMg(e.colesterolTotal());
			}
		}
		return new EnergiaResultado(metodo,geb,get,vet,kcalKg,carbo,ptn,lip,dislip,colesterol,motivos.isEmpty()?null:motivos);
	}

	private CalculadoraGetPorMet.Resultado met(RotinaMet rotina,String sexo,BigDecimal peso,BigDecimal magra){
		var lista=new ArrayList<CalculadoraGetPorMet.AtividadeDia>();var i=0;
		for(var a:rotina.atividades()==null?List.<AtividadeDiaria>of():rotina.atividades()){
			var atividade=atividades.buscar(a.codigo()).orElseThrow(()->invalido("energia.rotinaMet.atividades","Atividade desconhecida: "+a.codigo()));
			lista.add(new CalculadoraGetPorMet.AtividadeDia(atividade.met(),a.minutos()));i++;
		}
		try{
			return CalculadoraGetPorMet.calcular(new CalculadoraGetPorMet.Entrada(sexo,peso,magra,rotina.horasSono(),rotina.horasTrabalho(),rotina.intensidadeTrabalho(),lista));
		}catch(IllegalArgumentException ex){throw invalido("energia.rotinaMet",ex.getMessage());}
	}
	private static ExcecaoNegocio invalido(String campo,String mensagem){
		return new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null,mensagem,List.of(new ErroCampoResposta(campo,"FORMATO_INVALIDO",mensagem)),null);
	}
	private static BigDecimal massaMagra(SalvarAvaliacaoRequisicao r,ResultadosAvaliacao res){
		if(res!=null&&res.pesoMagroKg()!=null)return res.pesoMagroKg();
		return r.composicao()==null?null:r.composicao().pesoMagroKg();
	}
	private static MetodoGet metodo(EnergiaAvaliacao e){return e==null||e.metodo()==null?METODO_PADRAO:e.metodo();}
}
