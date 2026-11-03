package com.br.dietamais.comum.api;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Resultado por mutação: APLICADA (com a versão da entidade), CONFLITO (com o estado atual) ou REJEITADA (com código e regra). */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ResultadoMutacao(UUID idMutacao,Status status,Long versao,String codigo,String regra,JsonNode atual) {
	public enum Status { APLICADA, CONFLITO, REJEITADA }
	public static ResultadoMutacao aplicada(UUID idMutacao,long versao){return new ResultadoMutacao(idMutacao,Status.APLICADA,versao,null,null,null);}
	public static ResultadoMutacao rejeitada(UUID idMutacao,String codigo,String regra){return new ResultadoMutacao(idMutacao,Status.REJEITADA,null,codigo,regra,null);}
}
