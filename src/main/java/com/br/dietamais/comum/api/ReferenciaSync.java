package com.br.dietamais.comum.api;

import java.util.UUID;

public record ReferenciaSync(long seq,UUID id,UUID nutricionistaId,UUID pacienteId) {
	public ReferenciaSync {if(seq<1||id==null)throw new IllegalArgumentException("Referência sync inválida");}
}
