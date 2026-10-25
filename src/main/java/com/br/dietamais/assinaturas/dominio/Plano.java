package com.br.dietamais.assinaturas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plano")
public class Plano {
	@Id
	@Column(name = "id", nullable = false)
	private UUID id;
	@Column(name = "codigo", length = 40, nullable = false)
	private String codigo;
	@Column(name = "nome", length = 100, nullable = false)
	private String nome;
	@Column(name = "preco_mensal", precision = 10, scale = 2, nullable = false)
	private BigDecimal precoMensal;
	@Column(name = "limite_pacientes", nullable = false)
	private Integer limitePacientes;
	@Column(name = "ativo", nullable = false)
	private boolean ativo;
	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;
	@Column(name = "duracao_dias")
	private Integer duracaoDias;

	protected Plano() { }

	public UUID getId() { return id; }

	public String getCodigo() { return codigo; }

	public String getNome() { return nome; }

	public BigDecimal getPrecoMensal() { return precoMensal; }

	public Integer getLimitePacientes() { return limitePacientes; }

	public boolean getAtivo() { return ativo; }

	public Instant getCriadoEm() { return criadoEm; }

	public Integer getDuracaoDias() { return duracaoDias; }
}
