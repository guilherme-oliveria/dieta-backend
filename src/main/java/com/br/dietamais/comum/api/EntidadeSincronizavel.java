package com.br.dietamais.comum.api;

import java.time.Instant;
import com.br.dietamais.comum.infra.OuvinteEntidadeSincronizavel;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
@EntityListeners(OuvinteEntidadeSincronizavel.class)
public abstract class EntidadeSincronizavel {
	@Version
	@Column(nullable = false)
	private Long versao;
	@Column(nullable = false)
	private long seq;
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;
	@Column(name = "excluido_em")
	private Instant excluidoEm;

	public Long getVersao() { return versao; }
	public long getSeq() { return seq; }
	public Instant getCriadoEm() { return criadoEm; }
	public Instant getAtualizadoEm() { return atualizadoEm; }
	public Instant getExcluidoEm() { return excluidoEm; }

	public void excluir(Instant agora) {
		excluidoEm = java.util.Objects.requireNonNull(agora);
	}

	public void registrarEscrita(long sequencia, Instant agora) {
		if (criadoEm == null) criadoEm = agora;
		atualizadoEm = agora;
		seq = sequencia;
	}
}
