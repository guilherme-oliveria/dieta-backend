package com.br.dietamais.comum.api;

import java.time.Instant;

public record TermosUsoResposta(String versao,String status,boolean aprovado,boolean publicado,boolean cadastroHabilitado,
		Instant aprovadoEm,Instant publicadoEm,String conteudo,String url) { }
