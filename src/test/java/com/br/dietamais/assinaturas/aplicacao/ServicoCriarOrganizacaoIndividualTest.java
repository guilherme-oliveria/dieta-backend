package com.br.dietamais.assinaturas.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.br.dietamais.assinaturas.api.StatusAssinatura;
import com.br.dietamais.assinaturas.dominio.Assinatura;
import com.br.dietamais.assinaturas.dominio.Organizacao;
import com.br.dietamais.assinaturas.dominio.Plano;
import com.br.dietamais.assinaturas.infra.AssinaturaRepository;
import com.br.dietamais.assinaturas.infra.OrganizacaoRepository;
import com.br.dietamais.assinaturas.infra.PlanoRepository;
import com.br.dietamais.comum.api.GeradorIds;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServicoCriarOrganizacaoIndividualTest {
	private final PlanoRepository planos = mock(PlanoRepository.class);
	private final OrganizacaoRepository organizacoes = mock(OrganizacaoRepository.class);
	private final AssinaturaRepository assinaturas = mock(AssinaturaRepository.class);
	private final GeradorIds ids = mock(GeradorIds.class);

	@Test
	void duracaoELimiteVemDoPlanoSemConstanteDeTrintaDias() {
		var plano = mock(Plano.class);
		when(plano.getCodigo()).thenReturn("TESTE");
		when(plano.getId()).thenReturn(UUID.fromString("019a1400-0000-7000-8000-000000000001"));
		when(plano.getDuracaoDias()).thenReturn(45);
		when(plano.getLimitePacientes()).thenReturn(7);
		when(planos.findByCodigoAndAtivoTrue("TESTE")).thenReturn(Optional.of(plano));
		when(ids.novo()).thenReturn(UUID.fromString("019a1400-0000-7000-8000-000000000002"), UUID.fromString("019a1400-0000-7000-8000-000000000003"));
		when(organizacoes.saveAndFlush(any(Organizacao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		Instant agora = Instant.parse("2026-10-25T12:00:00Z");
		var resposta = new ServicoCriarOrganizacaoIndividual(planos, organizacoes, assinaturas, ids).criar("Nutri fictícia", agora);
		assertThat(resposta.fimPeriodoEm()).isEqualTo(Instant.parse("2026-12-09T12:00:00Z"));
		assertThat(resposta.limitePacientes()).isEqualTo(7);
		assertThat(resposta.status()).isEqualTo(StatusAssinatura.TESTE);
		var registro = ArgumentCaptor.forClass(Assinatura.class);
		verify(assinaturas).saveAndFlush(registro.capture());
		assertThat(registro.getValue().getInicioEm()).isEqualTo(agora);
		assertThat(registro.getValue().getFimPeriodoEm()).isEqualTo(resposta.fimPeriodoEm());
		var organizacao = ArgumentCaptor.forClass(Organizacao.class);
		verify(organizacoes).saveAndFlush(organizacao.capture());
		assertThat(organizacao.getValue().getTipo()).isEqualTo("INDIVIDUAL");
	}

	@Test
	void planoInexistenteNaoCriaOrganizacaoNemAssinatura() {
		when(planos.findByCodigoAndAtivoTrue("TESTE")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> new ServicoCriarOrganizacaoIndividual(planos, organizacoes, assinaturas, ids)
				.criar("Nutri fictícia", Instant.parse("2026-10-25T12:00:00Z"))).isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(organizacoes, assinaturas, ids);
	}
}
