package com.br.dietamais.questionarios.dominio;

import com.br.dietamais.questionarios.api.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Modelo padrão que toda nutri recebe (RN-ANA-01): objetivo, histórico, familiar, alergias, preferências, treino, rotina, recordatório, frequência, hábitos, digestão e saúde da mulher. */
public final class ModeloPadrao {
	public static final String NOME="Anamnese completa";
	private ModeloPadrao(){}

	public static List<BlocoQuestionario> blocos(Supplier<UUID> ids){
		var sexo=ids.get();var blocos=new ArrayList<BlocoQuestionario>();
		blocos.add(bloco("Objetivo",
			p(ids,"Qual é o seu principal objetivo com a dieta?",TipoPergunta.ESCOLHA_UNICA,true,"Emagrecer","Ganhar massa muscular","Melhorar a saúde","Controlar uma condição clínica","Outro"),
			p(ids,"Conte com suas palavras o que você espera do acompanhamento.",TipoPergunta.TEXTO,false),
			new PerguntaQuestionario(sexo,"Sexo",TipoPergunta.ESCOLHA_UNICA,List.of("Feminino","Masculino","Prefiro não informar"),true,null)));
		blocos.add(bloco("Histórico de saúde",
			p(ids,"Quais são suas queixas atuais?",TipoPergunta.TEXTO,false),
			p(ids,"Apresenta transtornos alimentares?",TipoPergunta.TEXTO,false),
			p(ids,"Você tem alguma doença diagnosticada?",TipoPergunta.TEXTO,false),
			p(ids,"Usa algum medicamento ou suplemento? Quais?",TipoPergunta.TEXTO,false),
			p(ids,"Já fez alguma cirurgia?",TipoPergunta.TEXTO,false),
			p(ids,"Tem exames recentes ou outros cuidados de saúde?",TipoPergunta.TEXTO,false),
			p(ids,"Realizou tratamentos recentemente? Quais?",TipoPergunta.TEXTO,false),
			c(ids,"PA_SISTOLICA","Pressão sistólica (mmHg)"),c(ids,"PA_DIASTOLICA","Pressão diastólica (mmHg)")));
		blocos.add(bloco("Histórico familiar",
			p(ids,"Há casos de diabetes, hipertensão, colesterol alto ou obesidade na família?",TipoPergunta.MULTIPLA_ESCOLHA,false,"Diabetes","Hipertensão","Colesterol alto","Obesidade","Doença cardíaca","Nenhum")));
		blocos.add(bloco("Alergias e intolerâncias",
			p(ids,"Tem alergia ou intolerância a algum alimento?",TipoPergunta.TEXTO,false),
			p(ids,"Quais alimentos você não come de jeito nenhum?",TipoPergunta.TEXTO,false)));
		blocos.add(bloco("Preferências",
			p(ids,"Quais alimentos você mais gosta?",TipoPergunta.TEXTO,false),
			p(ids,"Você cozinha ou come fora com frequência?",TipoPergunta.ESCOLHA_UNICA,false,"Cozinho em casa","Como fora","Os dois"),
			p(ids,"Como classifica seu apetite?",TipoPergunta.TEXTO,false),p(ids,"Em qual horário sente mais fome?",TipoPergunta.HORARIO,false),
			p(ids,"Belisca entre as refeições?",TipoPergunta.ESCOLHA_UNICA,false,"Sim","Não"),
			p(ids,"Houve alteração recente do apetite? Qual o motivo?",TipoPergunta.TEXTO,false),
			p(ids,"Quantidade de água pura ingerida por dia (ml)",TipoPergunta.NUMERO,false),
			p(ids,"Quantidade total de líquidos ingerida por dia (ml)",TipoPergunta.NUMERO,false),
			p(ids,"Ingere líquidos junto às refeições?",TipoPergunta.ESCOLHA_UNICA,false,"Sim","Não","Raramente"),
			p(ids,"Suplementos: quais e indicações de uso?",TipoPergunta.TEXTO,false),
			p(ids,"Como avalia a quantidade de sal e gordura da alimentação?",TipoPergunta.TEXTO,false)));
		var treino=new ArrayList<>(List.of(
			p(ids,"Você pratica atividade física?",TipoPergunta.ESCOLHA_UNICA,true,"Não","1 a 2 vezes por semana","3 a 4 vezes por semana","5 ou mais vezes por semana"),
			p(ids,"Qual modalidade e em que horário você treina?",TipoPergunta.TEXTO,false),
			p(ids,"Horário habitual do treino",TipoPergunta.HORARIO,false)));
		String[] dias={"Segunda-feira","Terça-feira","Quarta-feira","Quinta-feira","Sexta-feira","Sábado","Domingo"};
		for(int i=0;i<dias.length;i++){
			var prefixo=CodigosAnamnese.prefixoAtividade(CodigosAnamnese.DIAS.get(i));
			treino.add(p(ids,dias[i]+": exercício e frequência semanal",TipoPergunta.TEXTO,false));
			treino.add(c(ids,prefixo+"INICIO",dias[i]+": horário inicial"));treino.add(c(ids,prefixo+"FIM",dias[i]+": horário final"));
		}
		blocos.add(new BlocoQuestionario("Treino",treino));
		blocos.add(bloco("Rotina",
			p(ids,"Que horas você costuma acordar?",TipoPergunta.HORARIO,false),
			p(ids,"Que horas você costuma dormir?",TipoPergunta.HORARIO,false),
			p(ids,"Como é a sua rotina de trabalho ou estudo?",TipoPergunta.TEXTO,false),
			p(ids,"Dorme bem?",TipoPergunta.ESCOLHA_UNICA,false,"Sim","Não"),
			p(ids,"Quantas horas você dorme por noite?",TipoPergunta.NUMERO,false)));
		blocos.add(bloco("Recordatório de 24 horas",
			p(ids,"Descreva tudo o que você comeu e bebeu ontem, por refeição.",TipoPergunta.LISTA_POR_REFEICAO,false)));
		// av. alimentar!A93:A112 das três planilhas: 20 grupos, sete frequências em B92:H92.
		String[] frequencias={"Arroz, vegetal C e similares","Pão, farinhas e similares","Cereais e derivados integrais","Hortaliças","Frutas","Sucos","Feijão e similares","Carne bovina","Carne de aves","Carne suína","Outras carnes","Ovos","Embutidos","Leite e derivados","Castanhas","Açúcar e doces","Frituras/Gorduras","Refrigerantes","Adoçante","Produtos diet & light"};
		var frequencia=new ArrayList<PerguntaQuestionario>();
		var opcoesFrequencia=List.of("Mais de uma vez ao dia","Uma vez ao dia","3 a 6 vezes por semana","1 a 2 vezes por semana","Quinzenal","Mensal","Raro ou nunca");
		for(int i=0;i<frequencias.length;i++)frequencia.add(new PerguntaQuestionario(ids.get(),frequencias[i],TipoPergunta.ESCOLHA_UNICA,opcoesFrequencia,false,null,"FREQUENCIA_"+(i+1)));
		blocos.add(new BlocoQuestionario("Frequência alimentar",frequencia));
		var habitos=new ArrayList<>(List.of(
			p(ids,"Quantos copos de água você bebe por dia?",TipoPergunta.NUMERO,false),
			p(ids,"Você fuma?",TipoPergunta.ESCOLHA_UNICA,false,"Não","Sim","Parei"),
			p(ids,"Quantos cigarros você fuma por dia e há quanto tempo?",TipoPergunta.TEXTO,false),
			p(ids,"Ingere bebidas alcoólicas?",TipoPergunta.ESCOLHA_UNICA,false,"Sim","Não"),
			p(ids,"Como você avalia o seu nível de estresse?",TipoPergunta.ESCALA_1_5,false),
			c(ids,"SAL_GRAMAS_MES","Sal consumido em casa no mês (g)"),c(ids,"SAL_PESSOAS","Pessoas que consomem esse sal"),
			c(ids,"OLEO_ML_MES","Óleo consumido em casa no mês (ml)"),c(ids,"OLEO_PESSOAS","Pessoas que consomem esse óleo")));
		CodigosAnamnese.BEBIDAS.forEach((nome,bebida)->{
			var prefixo=CodigosAnamnese.prefixo(bebida);
			habitos.add(c(ids,prefixo+"UNIDADE",nome+": medida"));habitos.add(c(ids,prefixo+"QUANTIDADE",nome+": medidas por vez"));habitos.add(c(ids,prefixo+"DIAS_SEMANA",nome+": frequência semanal"));
		});
		blocos.add(new BlocoQuestionario("Hábitos",habitos));
		blocos.add(bloco("Digestão",
			p(ids,"Como funciona o seu intestino?",TipoPergunta.ESCOLHA_UNICA,false,"Todo dia","Dia sim, dia não","Prisão de ventre","Diarreia frequente"),
			p(ids,"Como estão a urina e a frequência urinária?",TipoPergunta.TEXTO,false),
			p(ids,"Apresenta edema ou retenção de líquidos?",TipoPergunta.TEXTO,false),
			p(ids,"Sente azia, gases ou inchaço?",TipoPergunta.MULTIPLA_ESCOLHA,false,"Azia","Gases","Inchaço","Refluxo","Nenhum")));
		var cond=new CondicaoPergunta(sexo,OperadorCondicao.IGUAL,"Feminino");
		blocos.add(bloco("Saúde da mulher",
			new PerguntaQuestionario(ids.get(),"Seu ciclo menstrual é regular?",TipoPergunta.ESCOLHA_UNICA,List.of("Sim","Não","Não menstruo"),false,cond),
			new PerguntaQuestionario(ids.get(),"Usa anticoncepcional?",TipoPergunta.ESCOLHA_UNICA,List.of("Sim","Não"),false,cond),
			new PerguntaQuestionario(ids.get(),"Está grávida ou amamentando?",TipoPergunta.ESCOLHA_UNICA,List.of("Não","Grávida","Amamentando"),false,cond)));
		return blocos;
	}
	private static BlocoQuestionario bloco(String titulo,PerguntaQuestionario... perguntas){return new BlocoQuestionario(titulo,List.of(perguntas));}
	private static PerguntaQuestionario c(Supplier<UUID> ids,String codigo,String texto){
		return new PerguntaQuestionario(ids.get(),texto,CodigosAnamnese.tipo(codigo),CodigosAnamnese.opcoes(codigo),false,null,codigo);
	}
	private static PerguntaQuestionario p(Supplier<UUID> ids,String texto,TipoPergunta tipo,boolean obrigatoria,String... opcoes){
		return new PerguntaQuestionario(ids.get(),texto,tipo,opcoes.length==0?null:List.of(opcoes),obrigatoria,null);
	}
}
