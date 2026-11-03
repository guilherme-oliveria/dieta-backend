package com.br.dietamais.contas.dominio;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.api.AtualizarPerfilNutriRequisicao;

public record DadosPerfilNutri(String nome,String crn,String telefone,String orientacaoPadrao) {
	public DadosPerfilNutri {
		nome=limpar(nome);crn=limpar(crn);telefone=limpar(telefone);orientacaoPadrao=limpar(orientacaoPadrao);
		if(nome==null||nome.length()>150||crn==null||crn.length()>20||!crn.matches("CRN-[1-9][0-9]* [0-9]+")
				||telefone!=null&&telefone.length()>30||orientacaoPadrao!=null&&orientacaoPadrao.length()>10000)
			throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
	}
	public static DadosPerfilNutri de(AtualizarPerfilNutriRequisicao r){return new DadosPerfilNutri(r.nome(),r.crn(),r.telefone(),r.orientacaoPadrao());}
	private static String limpar(String valor){return valor==null||valor.isBlank()?null:valor.strip();}
	@Override public String toString(){return "DadosPerfilNutri[conteúdo protegido]";}
}
