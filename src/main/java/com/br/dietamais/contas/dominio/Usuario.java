package com.br.dietamais.contas.dominio;

import com.br.dietamais.comum.api.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {
	@Id
	@Column(name = "id", nullable = false)
	private UUID id;
	@Column(name = "email", length = 255, nullable = false)
	private String email;
	@Column(name = "senha_hash", length = 255, nullable = false)
	private String senhaHash;
	@Column(name = "nome", length = 150, nullable = false)
	private String nome;
	@Enumerated(EnumType.STRING)
	@Column(name = "perfil", length = 20, nullable = false)
	private Perfil perfil;
	@Column(name = "ativo", nullable = false)
	private boolean ativo;
	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;
	@Column(name = "email_confirmado_em")
	private Instant emailConfirmadoEm;
	@Column(name = "fuso", length = 80, nullable = false)
	private String fuso;
	@Column(name = "termos_aceitos_em")
	private Instant termosAceitosEm;
	@Column(name = "versao_termos", length = 40)
	private String versaoTermos;

	protected Usuario() { }

	public UUID getId() { return id; }

	public String getEmail() { return email; }

	public String getSenhaHash() { return senhaHash; }

	public String getNome() { return nome; }

	public Perfil getPerfil() { return perfil; }

	public boolean getAtivo() { return ativo; }

	public Instant getCriadoEm() { return criadoEm; }

	public Instant getAtualizadoEm() { return atualizadoEm; }

	public Instant getEmailConfirmadoEm() { return emailConfirmadoEm; }

	public String getFuso() { return fuso; }

	public Instant getTermosAceitosEm() { return termosAceitosEm; }

	public String getVersaoTermos() { return versaoTermos; }
}
