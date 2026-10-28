package com.br.dietamais.comum.api;

import jakarta.validation.Validator;
import java.util.UUID;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code POST /sync} da nutri (BE-25): aplica a mesma regra do endpoint REST da entidade. {@code dados} é o corpo do REST
 * ({@code CRIAR}: o {@code id} da mutação vira o id da entidade; {@code ATUALIZAR}: {@code versaoBase} faz o papel do If-Match).
 * Conflito, limite de plano e demais regras voltam por item (RN-SYN-02, RN-SYN-04). Não há {@code EXCLUIR}: nenhuma dessas entidades se exclui.
 */
public final class ManipuladorEscritaNutri<C,A> implements ManipuladorMutacao {
	/** De onde sai a ficha do paciente na criação. */
	public enum Paciente { NENHUM, NO_DTO, FORA_DO_DTO }
	@FunctionalInterface public interface Criar<C> { long criar(UUID ficha,C requisicao); }
	@FunctionalInterface public interface Atualizar<A> { long atualizar(UUID id,String ifMatch,A requisicao); }

	private final String entidade;private final JsonMapper mapper;private final Validator validador;private final UsuarioAtual usuario;
	private final Class<C> classeCriar;private final Criar<C> criar;private final Paciente paciente;private final Class<A> classeAtualizar;private final Atualizar<A> atualizar;

	private ManipuladorEscritaNutri(String entidade,JsonMapper mapper,Validator validador,UsuarioAtual usuario,Class<C> classeCriar,Criar<C> criar,Paciente paciente,Class<A> classeAtualizar,Atualizar<A> atualizar){
		this.entidade=entidade;this.mapper=mapper;this.validador=validador;this.usuario=usuario;this.classeCriar=classeCriar;this.criar=criar;this.paciente=paciente;this.classeAtualizar=classeAtualizar;this.atualizar=atualizar;
	}
	public static <C,A> ManipuladorEscritaNutri<C,A> de(String entidade,JsonMapper mapper,Validator validador,UsuarioAtual usuario,Class<C> classeCriar,Paciente paciente,Criar<C> criar,Class<A> classeAtualizar,Atualizar<A> atualizar){
		return new ManipuladorEscritaNutri<>(entidade,mapper,validador,usuario,classeCriar,criar,paciente,classeAtualizar,atualizar);
	}
	/** Entidade que só se atualiza (ex.: lista de substituição, que nasce com a ficha). */
	public static <A> ManipuladorEscritaNutri<Void,A> soAtualizacao(String entidade,JsonMapper mapper,Validator validador,UsuarioAtual usuario,Class<A> classeAtualizar,Atualizar<A> atualizar){
		return new ManipuladorEscritaNutri<>(entidade,mapper,validador,usuario,null,null,Paciente.NENHUM,classeAtualizar,atualizar);
	}

	@Override public String entidade(){return entidade;}

	@Override public ResultadoMutacao aplicar(MutacaoSync m,ContextoUsuario contexto){
		contexto.exigirNutricionista();
		if(m.dados()==null||!m.dados().isObject())throw formato();
		long versao=switch(m.operacao()){
			case CRIAR->{if(criar==null)throw naoSuportada();yield aplicarCriar(m);}
			case ATUALIZAR->aplicarAtualizar(m);
			case EXCLUIR->throw naoSuportada();
		};
		return ResultadoMutacao.aplicada(m.idMutacao(),versao);
	}

	private long aplicarCriar(MutacaoSync m){
		var corpo=(ObjectNode)m.dados().deepCopy();corpo.put("id",m.id().toString());
		UUID ficha=null;
		if(paciente!=Paciente.NENHUM){
			var texto=corpo.path("pacienteId").asString(null);if(texto==null)throw formato();
			try{ficha=UUID.fromString(texto);}catch(IllegalArgumentException e){throw formato();}
			if(paciente==Paciente.FORA_DO_DTO)corpo.remove("pacienteId");
		}
		return criar.criar(ficha,ler(corpo,classeCriar));
	}
	private long aplicarAtualizar(MutacaoSync m){
		if(m.versaoBase()==null)throw formato();
		var corpo=(ObjectNode)m.dados().deepCopy();
		return atualizar.atualizar(m.id(),"\""+m.versaoBase()+"\"",ler(corpo,classeAtualizar));
	}
	private <T> T ler(ObjectNode corpo,Class<T> classe){
		T valor;
		try{valor=mapper.treeToValue(corpo,classe);}catch(RuntimeException e){throw formato();}
		if(!validador.validate(valor).isEmpty())throw formato();
		return valor;
	}
	private static ExcecaoNegocio formato(){return new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-04");}
	private static ExcecaoNegocio naoSuportada(){return new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-04");}
}
