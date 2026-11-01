package com.br.dietamais.dietas.api;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RefeicaoRequisicao(
	UUID id,
	@NotBlank @Size(max=150) String nome,
	@NotNull LocalTime horario,
	@PositiveOrZero int ordem,
	@NotNull List<@NotNull @Valid ItemRefeicaoRequisicao> itens,
	/** `PRINCIPAL` (padrão) ou `ALTERNATIVA` (RN-DIE-11). */
	@Pattern(regexp="PRINCIPAL|ALTERNATIVA") String tipo,
	/** Id (informado nesta mesma requisição) da refeição principal que esta alternativa substitui; só com `tipo=ALTERNATIVA`. */
	UUID alternativaDe) {
	public RefeicaoRequisicao(UUID id,String nome,LocalTime horario,int ordem,List<ItemRefeicaoRequisicao> itens){this(id,nome,horario,ordem,itens,null,null);}
}
