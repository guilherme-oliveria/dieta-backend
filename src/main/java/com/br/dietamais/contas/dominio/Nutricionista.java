package com.br.dietamais.contas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "nutricionista")
public class Nutricionista {
	@Id
	@Column(name = "id", nullable = false)
	private UUID id;
	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;
	@Column(name = "organizacao_id", nullable = false)
	private UUID organizacaoId;
	@Column(name = "crn", length = 20, nullable = false)
	private String crn;
	@Column(name = "telefone", length = 20)
	private String telefone;
	@Column(name = "crn_verificado", nullable = false)
	private boolean crnVerificado;

	protected Nutricionista() { }

	public Nutricionista(UUID id, UUID usuarioId, UUID organizacaoId, String crn) {
		this.id = id;
		this.usuarioId = usuarioId;
		this.organizacaoId = organizacaoId;
		this.crn = crn;
		this.crnVerificado = false;
	}

	public UUID getId() { return id; }

	public UUID getUsuarioId() { return usuarioId; }

	public UUID getOrganizacaoId() { return organizacaoId; }

	public String getCrn() { return crn; }

	public String getTelefone() { return telefone; }

	public boolean getCrnVerificado() { return crnVerificado; }
}
