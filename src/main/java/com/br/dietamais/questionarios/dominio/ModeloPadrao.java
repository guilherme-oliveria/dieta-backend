package com.br.dietamais.questionarios.dominio;

import com.br.dietamais.questionarios.api.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Modelo padrão que toda nutri recebe (RN-ANA-01): objetivo, histórico, familiar, alergias, preferências, treino, rotina, recordatório, frequência, hábitos, digestão e saúde da mulher. */
public final class ModeloPadrao {
	public static final String NOME="Anamnese padrão";
	private ModeloPadrao(){}

	public static List<BlocoQuestionario> blocos(Supplier<UUID> ids){
		var sexo=ids.get();var blocos=new ArrayList<BlocoQuestionario>();
		blocos.add(bloco("Objetivo",
			p(ids,"Qual é o seu principal objetivo com a dieta?",TipoPergunta.ESCOLHA_UNICA,true,"Emagrecer","Ganhar massa muscular","Melhorar a saúde","Controlar uma condição clínica","Outro"),
			p(ids,"Conte com suas palavras o que você espera do acompanhamento.",TipoPergunta.TEXTO,false),
			new PerguntaQuestionario(sexo,"Sexo",TipoPergunta.ESCOLHA_UNICA,List.of("Feminino","Masculino","Prefiro não informar"),true,null)));
		blocos.add(bloco("Histórico de saúde",
			p(ids,"Você tem alguma doença diagnosticada?",TipoPergunta.TEXTO,false),
			p(ids,"Usa algum medicamento ou suplemento? Quais?",TipoPergunta.TEXTO,false),
			p(ids,"Já fez alguma cirurgia?",TipoPergunta.TEXTO,false)));
		blocos.add(bloco("Histórico familiar",
			p(ids,"Há casos de diabetes, hipertensão, colesterol alto ou obesidade na família?",TipoPergunta.MULTIPLA_ESCOLHA,false,"Diabetes","Hipertensão","Colesterol alto","Obesidade","Doença cardíaca","Nenhum")));
		blocos.add(bloco("Alergias e intolerâncias",
			p(ids,"Tem alergia ou intolerância a algum alimento?",TipoPergunta.TEXTO,false),
			p(ids,"Quais alimentos você não come de jeito nenhum?",TipoPergunta.TEXTO,false)));
		blocos.add(bloco("Preferências",
			p(ids,"Quais alimentos você mais gosta?",TipoPergunta.TEXTO,false),
			p(ids,"Você cozinha ou come fora com frequência?",TipoPergunta.ESCOLHA_UNICA,false,"Cozinho em casa","Como fora","Os dois")));
		blocos.add(bloco("Treino",
			p(ids,"Você pratica atividade física?",TipoPergunta.ESCOLHA_UNICA,true,"Não","1 a 2 vezes por semana","3 a 4 vezes por semana","5 ou mais vezes por semana"),
			p(ids,"Qual modalidade e em que horário você treina?",TipoPergunta.TEXTO,false),
			p(ids,"Horário habitual do treino",TipoPergunta.HORARIO,false)));
		blocos.add(bloco("Rotina",
			p(ids,"Que horas você costuma acordar?",TipoPergunta.HORARIO,false),
			p(ids,"Que horas você costuma dormir?",TipoPergunta.HORARIO,false),
			p(ids,"Como é a sua rotina de trabalho ou estudo?",TipoPergunta.TEXTO,false),
			p(ids,"Quantas horas você dorme por noite?",TipoPergunta.NUMERO,false)));
		blocos.add(bloco("Recordatório de 24 horas",
			p(ids,"Descreva tudo o que você comeu e bebeu ontem, por refeição.",TipoPergunta.LISTA_POR_REFEICAO,false)));
		blocos.add(bloco("Frequência alimentar",
			p(ids,"Frutas e verduras",TipoPergunta.ESCALA_1_5,false),
			p(ids,"Doces e sobremesas",TipoPergunta.ESCALA_1_5,false),
			p(ids,"Frituras",TipoPergunta.ESCALA_1_5,false),
			p(ids,"Refrigerantes e sucos industrializados",TipoPergunta.ESCALA_1_5,false),
			p(ids,"Bebida alcoólica",TipoPergunta.ESCALA_1_5,false)));
		blocos.add(bloco("Hábitos",
			p(ids,"Quantos copos de água você bebe por dia?",TipoPergunta.NUMERO,false),
			p(ids,"Você fuma?",TipoPergunta.ESCOLHA_UNICA,false,"Não","Sim","Parei"),
			p(ids,"Como você avalia o seu nível de estresse?",TipoPergunta.ESCALA_1_5,false)));
		blocos.add(bloco("Digestão",
			p(ids,"Como funciona o seu intestino?",TipoPergunta.ESCOLHA_UNICA,false,"Todo dia","Dia sim, dia não","Prisão de ventre","Diarreia frequente"),
			p(ids,"Sente azia, gases ou inchaço?",TipoPergunta.MULTIPLA_ESCOLHA,false,"Azia","Gases","Inchaço","Refluxo","Nenhum")));
		var cond=new CondicaoPergunta(sexo,OperadorCondicao.IGUAL,"Feminino");
		blocos.add(bloco("Saúde da mulher",
			new PerguntaQuestionario(ids.get(),"Seu ciclo menstrual é regular?",TipoPergunta.ESCOLHA_UNICA,List.of("Sim","Não","Não menstruo"),false,cond),
			new PerguntaQuestionario(ids.get(),"Usa anticoncepcional?",TipoPergunta.ESCOLHA_UNICA,List.of("Sim","Não"),false,cond),
			new PerguntaQuestionario(ids.get(),"Está grávida ou amamentando?",TipoPergunta.ESCOLHA_UNICA,List.of("Não","Grávida","Amamentando"),false,cond)));
		return blocos;
	}
	private static BlocoQuestionario bloco(String titulo,PerguntaQuestionario... perguntas){return new BlocoQuestionario(titulo,List.of(perguntas));}
	private static PerguntaQuestionario p(Supplier<UUID> ids,String texto,TipoPergunta tipo,boolean obrigatoria,String... opcoes){
		return new PerguntaQuestionario(ids.get(),texto,tipo,opcoes.length==0?null:List.of(opcoes),obrigatoria,null);
	}
}
