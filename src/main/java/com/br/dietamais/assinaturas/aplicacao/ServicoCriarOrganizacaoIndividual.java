package com.br.dietamais.assinaturas.aplicacao;

import java.time.Duration;
import java.time.Instant;
import com.br.dietamais.assinaturas.api.AssinaturaResumoResposta;
import com.br.dietamais.assinaturas.api.CriarOrganizacaoIndividual;
import com.br.dietamais.assinaturas.api.StatusAssinatura;
import com.br.dietamais.assinaturas.dominio.Assinatura;
import com.br.dietamais.assinaturas.dominio.Organizacao;
import com.br.dietamais.assinaturas.infra.AssinaturaRepository;
import com.br.dietamais.assinaturas.infra.OrganizacaoRepository;
import com.br.dietamais.assinaturas.infra.PlanoRepository;
import com.br.dietamais.comum.api.GeradorIds;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoCriarOrganizacaoIndividual implements CriarOrganizacaoIndividual {
	private final PlanoRepository planos;
	private final OrganizacaoRepository organizacoes;
	private final AssinaturaRepository assinaturas;
	private final GeradorIds ids;

	public ServicoCriarOrganizacaoIndividual(PlanoRepository planos, OrganizacaoRepository organizacoes,
			AssinaturaRepository assinaturas, GeradorIds ids) {
		this.planos = planos;
		this.organizacoes = organizacoes;
		this.assinaturas = assinaturas;
		this.ids = ids;
	}

	@Override
	@Transactional
	public AssinaturaResumoResposta criar(String nome, Instant agora) {
		var plano = planos.findByCodigoAndAtivoTrue("TESTE")
				.orElseThrow(() -> new IllegalStateException("Plano de teste não configurado"));
		if (plano.getDuracaoDias() == null || plano.getDuracaoDias() <= 0 || plano.getLimitePacientes() < 0) {
			throw new IllegalStateException("Plano de teste inválido");
		}
		var organizacao = organizacoes.saveAndFlush(new Organizacao(ids.novo(), nome, agora));
		Instant fim = agora.plus(Duration.ofDays(plano.getDuracaoDias()));
		assinaturas.saveAndFlush(new Assinatura(ids.novo(), organizacao.getId(), plano.getId(), agora, fim));
		return new AssinaturaResumoResposta(organizacao.getId(), plano.getCodigo(), StatusAssinatura.TESTE,
				plano.getLimitePacientes(), 0, fim);
	}
}
