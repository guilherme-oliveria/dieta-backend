package com.br.dietamais.dietas.dominio;

import com.br.dietamais.dietas.api.StatusDieta;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public final class LinhasDieta {
	private LinhasDieta(){}
	public record Dieta(UUID id,UUID vinculoId,UUID pacienteId,String titulo,String orientacao,StatusDieta status,long versao,Instant criadoEm,Instant atualizadoEm) {}
	public record Refeicao(UUID id,UUID dietaId,String nome,LocalTime horario,int ordem) {}
	public record Item(UUID id,UUID refeicaoId,UUID alimentoId,BigDecimal porcoes,String observacao,int ordem) {}
}
