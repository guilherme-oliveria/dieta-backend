package com.br.dietamais.questionarios.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.api.SalvarRespostasRequisicao;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code POST /sync} do paciente: {@code ATUALIZAR} em {@code questionarios} salva respostas parciais (RN-ANA-03). Mesmas regras do endpoint REST.
 * As respostas mesclam por pergunta, então {@code versaoBase} não gera conflito. Concluir o questionário exige online.
 */
@Component
public class ManipuladorRespostasSync implements ManipuladorMutacao {
	private final GerenciarQuestionarios questionarios;private final JsonMapper mapper;private final Validator validador;
	public ManipuladorRespostasSync(GerenciarQuestionarios questionarios,JsonMapper mapper,Validator validador){this.questionarios=questionarios;this.mapper=mapper;this.validador=validador;}
	@Override public String entidade(){return "questionarios";}
	@Override public ResultadoMutacao aplicar(MutacaoSync m,ContextoUsuario usuario){
		usuario.exigirPaciente();
		if(m.operacao()!=OperacaoSync.ATUALIZAR||m.id()==null||m.dados()==null||m.dados().isNull())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-03");
		SalvarRespostasRequisicao r;
		try{r=mapper.treeToValue(m.dados(),SalvarRespostasRequisicao.class);}catch(RuntimeException e){throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);}
		if(!validador.validate(r).isEmpty())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		return ResultadoMutacao.aplicada(m.idMutacao(),questionarios.salvarRespostas(m.id(),r).versao());
	}
}
