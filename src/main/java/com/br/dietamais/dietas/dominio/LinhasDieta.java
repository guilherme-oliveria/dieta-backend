package com.br.dietamais.dietas.dominio;

import com.br.dietamais.dietas.api.StatusDieta;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public final class LinhasDieta {
	private LinhasDieta(){}
	public record Dieta(UUID id,UUID vinculoId,UUID pacienteId,String titulo,String orientacao,StatusDieta status,long versao,Instant criadoEm,Instant atualizadoEm,String porcoesJson,String metasJson,BigDecimal pesoReferenciaKg,String rodape,java.util.List<String> restricoes) {
		public Dieta(UUID id,UUID vinculoId,UUID pacienteId,String titulo,String orientacao,StatusDieta status,long versao,Instant criadoEm,Instant atualizadoEm){this(id,vinculoId,pacienteId,titulo,orientacao,status,versao,criadoEm,atualizadoEm,null,null,null,null,java.util.List.of());}
	}
	public record Refeicao(UUID id,UUID dietaId,String nome,LocalTime horario,int ordem,String tipo,UUID alternativaDe) {}
	public record Item(UUID id,UUID refeicaoId,UUID alimentoId,UUID grupoId,BigDecimal porcoes,boolean aVontade,String textoLivre,String observacao,int ordem) {}
}
