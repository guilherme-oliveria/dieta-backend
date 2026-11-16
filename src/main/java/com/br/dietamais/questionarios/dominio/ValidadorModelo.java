package com.br.dietamais.questionarios.dominio;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.api.*;
import java.util.*;

/** Estrutura do modelo (RN-ANA-01/02): limites, opções por tipo e condições que apontam só para perguntas anteriores. */
public final class ValidadorModelo {
	static final int MAX_PERGUNTAS=200;
	private ValidadorModelo(){}

	/** Devolve os blocos com todos os ids preenchidos (gerando os que faltam). Lança 400 {@code FORMATO_INVALIDO} com o campo que falhou. */
	public static List<BlocoQuestionario> normalizar(List<BlocoQuestionario> blocos,java.util.function.Supplier<UUID> ids){
		var vistos=new HashMap<UUID,PerguntaQuestionario>();var codigos=new HashSet<String>();var resultado=new ArrayList<BlocoQuestionario>();int total=0;
		for(int b=0;b<blocos.size();b++){
			var bloco=blocos.get(b);var perguntas=new ArrayList<PerguntaQuestionario>();
			for(int i=0;i<bloco.perguntas().size();i++){
				var p=bloco.perguntas().get(i);String campo="blocos["+b+"].perguntas["+i+"]";
				if(++total>MAX_PERGUNTAS)throw invalido(campo,"O modelo aceita no máximo "+MAX_PERGUNTAS+" perguntas.");
				UUID id=p.id()==null?ids.get():p.id();
				if(p.id()!=null&&p.id().version()!=7)throw invalido(campo+".id","Use um UUIDv7.");
				if(vistos.containsKey(id))throw invalido(campo+".id","Id de pergunta repetido.");
				var opcoes=p.opcoes()==null||p.opcoes().isEmpty()?null:p.opcoes().stream().map(String::strip).toList();
				boolean comOpcoes=p.tipo()==TipoPergunta.ESCOLHA_UNICA||p.tipo()==TipoPergunta.MULTIPLA_ESCOLHA;
				if(comOpcoes&&(opcoes==null||opcoes.size()<2))throw invalido(campo+".opcoes","Informe ao menos 2 opções.");
				if(!comOpcoes&&opcoes!=null)throw invalido(campo+".opcoes","Este tipo de pergunta não tem opções.");
				if(opcoes!=null&&new HashSet<>(opcoes).size()!=opcoes.size())throw invalido(campo+".opcoes","Opções repetidas.");
				if(p.codigo()!=null){
					if(!p.codigo().matches("[A-Z][A-Z0-9_]{0,79}")||!codigos.add(p.codigo()))throw invalido(campo+".codigo","Código inválido ou repetido.");
					var tipo=CodigosAnamnese.tipo(p.codigo());
					if(tipo!=null&&tipo!=p.tipo())throw invalido(campo+".tipo","Tipo incompatível com o código da anamnese.");
					var permitidas=CodigosAnamnese.opcoes(p.codigo());
					if(permitidas!=null&&!permitidas.containsAll(opcoes))throw invalido(campo+".opcoes","Opção incompatível com o código da anamnese.");
				}
				var condicao=p.condicao();
				if(condicao!=null){
					var alvo=vistos.get(condicao.perguntaId());
					if(alvo==null)throw invalido(campo+".condicao.perguntaId","A condição deve apontar para uma pergunta anterior do modelo.");
					if(alvo.opcoes()!=null&&!alvo.opcoes().contains(condicao.valor()))throw invalido(campo+".condicao.valor","Valor fora das opções da pergunta de referência.");
					if(condicao.operador()==OperadorCondicao.CONTEM&&alvo.tipo()!=TipoPergunta.MULTIPLA_ESCOLHA)throw invalido(campo+".condicao.operador","CONTEM só vale para múltipla escolha.");
				}
				var normal=new PerguntaQuestionario(id,p.texto().strip(),p.tipo(),opcoes,Boolean.TRUE.equals(p.obrigatoria()),condicao,p.codigo());
				vistos.put(id,normal);perguntas.add(normal);
			}
			resultado.add(new BlocoQuestionario(bloco.titulo().strip(),perguntas));
		}
		return resultado;
	}
	private static ExcecaoNegocio invalido(String campo,String mensagem){
		return new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ANA-02",mensagem,List.of(new ErroCampoResposta(campo,"FORMATO_INVALIDO",mensagem)),null);
	}
}
