package com.br.dietamais.assinaturas.dominio;

import com.br.dietamais.assinaturas.api.StatusAssinatura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assinatura")
public class Assinatura {
	@Id
	@Column(name = "id", nullable = false)
	private UUID id;
	@Column(name = "organizacao_id", nullable = false)
	private UUID organizacaoId;
	@Column(name = "plano_id", nullable = false)
	private UUID planoId;
	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private StatusAssinatura status;
	@Column(name = "inicio_em", nullable = false)
	private Instant inicioEm;
	@Column(name = "fim_periodo_em")
	private Instant fimPeriodoEm;
	@Column(name = "gateway_assinatura_id", length = 100)
	private String gatewayAssinaturaId;

	protected Assinatura() { }

	public Assinatura(UUID id, UUID organizacaoId, UUID planoId, Instant inicio, Instant fim) {
		this.id = id;
		this.organizacaoId = organizacaoId;
		this.planoId = planoId;
		this.status = StatusAssinatura.TESTE;
		this.inicioEm = inicio;
		this.fimPeriodoEm = fim;
	}

	public UUID getId() { return id; }

	public UUID getOrganizacaoId() { return organizacaoId; }

	public UUID getPlanoId() { return planoId; }

	public StatusAssinatura getStatus() { return status; }

	public Instant getInicioEm() { return inicioEm; }

	public Instant getFimPeriodoEm() { return fimPeriodoEm; }

	public String getGatewayAssinaturaId() { return gatewayAssinaturaId; }
}
