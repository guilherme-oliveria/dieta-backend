package com.br.dietamais.assinaturas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizacao")
public class Organizacao {
	@Id
	@Column(name = "id", nullable = false)
	private UUID id;
	@Column(name = "nome", length = 150, nullable = false)
	private String nome;
	@Column(name = "tipo", length = 20, nullable = false)
	private String tipo;
	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Organizacao() { }

	public Organizacao(UUID id, String nome, Instant agora) {
		this.id = id;
		this.nome = nome;
		this.tipo = "INDIVIDUAL";
		this.criadoEm = agora;
	}

	public UUID getId() { return id; }

	public String getNome() { return nome; }

	public String getTipo() { return tipo; }

	public Instant getCriadoEm() { return criadoEm; }
}
