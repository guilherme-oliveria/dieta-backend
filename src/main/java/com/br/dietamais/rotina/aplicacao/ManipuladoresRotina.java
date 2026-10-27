package com.br.dietamais.rotina.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.rotina.api.*;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Mutações CRIAR do paciente em {@code POST /sync} (RN-SYN-03): as mesmas regras e a mesma idempotência dos endpoints REST. */
public final class ManipuladoresRotina {
	private ManipuladoresRotina(){}
	abstract static class Base<T> implements ManipuladorMutacao {
		private final JsonMapper mapper;private final Validator validador;private final Class<T> tipo;
		Base(JsonMapper mapper,Validator validador,Class<T> tipo){this.mapper=mapper;this.validador=validador;this.tipo=tipo;}
		abstract T comId(T requisicao,java.util.UUID id);
		abstract long gravar(T requisicao);
		@Override public ResultadoMutacao aplicar(MutacaoSync m,ContextoUsuario usuario){
			usuario.exigirPaciente();
			if(m.operacao()!=OperacaoSync.CRIAR||m.id()==null||m.dados()==null||m.dados().isNull())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-03");
			T requisicao;
			try{requisicao=comId(mapper.treeToValue(m.dados(),tipo),m.id());}catch(RuntimeException e){throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);}
			if(!validador.validate(requisicao).isEmpty())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
			return ResultadoMutacao.aplicada(m.idMutacao(),gravar(requisicao));
		}
	}
	@Component static class Checkins extends Base<CheckinRequisicao> {
		private final RegistrarRotina rotina;
		Checkins(JsonMapper mapper,Validator validador,RegistrarRotina rotina){super(mapper,validador,CheckinRequisicao.class);this.rotina=rotina;}
		@Override public String entidade(){return "checkins";}
		@Override CheckinRequisicao comId(CheckinRequisicao r,java.util.UUID id){return new CheckinRequisicao(id,r.dietaId(),r.refeicaoId(),r.data(),r.status(),r.trocas(),r.registradoEm());}
		@Override long gravar(CheckinRequisicao r){rotina.checkin(r);return 1L;}
	}
	@Component static class Agua extends Base<AguaRequisicao> {
		private final RegistrarRotina rotina;
		Agua(JsonMapper mapper,Validator validador,RegistrarRotina rotina){super(mapper,validador,AguaRequisicao.class);this.rotina=rotina;}
		@Override public String entidade(){return "agua";}
		@Override AguaRequisicao comId(AguaRequisicao r,java.util.UUID id){return new AguaRequisicao(id,r.ml(),r.registradoEm());}
		@Override long gravar(AguaRequisicao r){rotina.agua(r);return 1L;}
	}
	@Component static class Pesos extends Base<PesoRequisicao> {
		private final RegistrarRotina rotina;
		Pesos(JsonMapper mapper,Validator validador,RegistrarRotina rotina){super(mapper,validador,PesoRequisicao.class);this.rotina=rotina;}
		@Override public String entidade(){return "pesos";}
		@Override PesoRequisicao comId(PesoRequisicao r,java.util.UUID id){return new PesoRequisicao(id,r.pesoKg(),r.registradoEm());}
		@Override long gravar(PesoRequisicao r){rotina.peso(r);return 1L;}
	}
}
