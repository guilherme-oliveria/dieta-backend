package com.br.dietamais.contas.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import com.br.dietamais.assinaturas.api.AssinaturaResumoResposta;
import com.br.dietamais.assinaturas.api.CriarOrganizacaoIndividual;
import com.br.dietamais.assinaturas.api.StatusAssinatura;
import com.br.dietamais.comum.api.CodigoErro;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import com.br.dietamais.comum.api.GeradorIds;
import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.contas.api.CadastroNutriRequisicao;
import com.br.dietamais.contas.dominio.Nutricionista;
import com.br.dietamais.contas.dominio.PoliticaSenha;
import com.br.dietamais.contas.infra.NutricionistaRepository;
import com.br.dietamais.contas.infra.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class CadastrarNutriTest {
	private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
	private final NutricionistaRepository nutricionistas = mock(NutricionistaRepository.class);
	private final CriarOrganizacaoIndividual organizacoes = mock(CriarOrganizacaoIndividual.class);
	private final PasswordEncoder encoder = mock(PasswordEncoder.class);
	private final GeradorIds ids = mock(GeradorIds.class);
	private final EmitirTokenConta emissor = mock(EmitirTokenConta.class);
	private final Instant agora = Instant.parse("2026-10-25T12:00:00Z");
	private final UUID usuarioId = UUID.fromString("019a1400-0000-7000-8000-000000000001");
	private final UUID nutriId = UUID.fromString("019a1400-0000-7000-8000-000000000002");
	private final UUID organizacaoId = UUID.fromString("019a1400-0000-7000-8000-000000000003");

	private CadastrarNutri caso() {
		return new CadastrarNutri(usuarios, nutricionistas, organizacoes, new PoliticaSenha(Set.of()), encoder,
				ids, Clock.fixed(agora, ZoneOffset.UTC), "America/Sao_Paulo", emissor);
	}

	private CadastroNutriRequisicao entrada(String fuso) {
		return new CadastroNutriRequisicao(" Nutri fictícia ", " NUTRI@EXAMPLE.INVALID ",
				"FRASE_FICTICIA_SEM_VALIDADE", "CRN-1 99999999", true, "2026-10", fuso);
	}

	@Test
	void usaHashERelogioControladoERetornaSomenteContaEAssinatura() {
		when(ids.novo()).thenReturn(usuarioId, nutriId);
		when(encoder.encode(anyString())).thenReturn("HASH_FICTICIO");
		when(usuarios.inserirSeEmailDisponivel(any(), anyString(), anyString(), anyString(), any(), anyString(), anyString())).thenReturn(1);
		var assinatura = new AssinaturaResumoResposta(organizacaoId, "TESTE", StatusAssinatura.TESTE,
				5, 0, Instant.parse("2026-11-24T12:00:00Z"));
		when(organizacoes.criar("Nutri fictícia", agora)).thenReturn(assinatura);
		when(nutricionistas.saveAndFlush(any(Nutricionista.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		var resposta = caso().executar(entrada(null));
		assertThat(resposta.usuario().id()).isEqualTo(usuarioId);
		assertThat(resposta.usuario().nutricionistaId()).isEqualTo(nutriId);
		assertThat(resposta.usuario().email()).isEqualTo("nutri@example.invalid");
		assertThat(resposta.usuario().emailConfirmado()).isFalse();
		assertThat(resposta.usuario().perfis()).containsExactly(Perfil.NUTRICIONISTA);
		assertThat(resposta.assinatura()).isEqualTo(assinatura);
		verify(encoder).encode("FRASE_FICTICIA_SEM_VALIDADE");
		verify(usuarios).inserirSeEmailDisponivel(usuarioId, "nutri@example.invalid", "HASH_FICTICIO",
				"Nutri fictícia", agora, "America/Sao_Paulo", "2026-10");
		var registro = ArgumentCaptor.forClass(Nutricionista.class);
		verify(nutricionistas).saveAndFlush(registro.capture());
		assertThat(registro.getValue().getOrganizacaoId()).isEqualTo(organizacaoId);
		assertThat(registro.getValue().getCrnVerificado()).isFalse();
	}

	@Test
	void emailOcupadoNaoCriaOrganizacaoOuIdentidadeNutri() {
		when(ids.novo()).thenReturn(usuarioId);
		when(encoder.encode(anyString())).thenReturn("HASH_FICTICIO");
		assertThatThrownBy(() -> caso().executar(entrada(null))).isInstanceOfSatisfying(ExcecaoNegocio.class, erro -> {
			assertThat(erro.codigo()).isEqualTo(CodigoErro.EMAIL_JA_CADASTRADO);
			assertThat(erro.regra()).isEqualTo("RN-CON-01");
		});
		verifyNoInteractions(organizacoes, nutricionistas);
	}

	@Test
	void fusoInvalidoERecusadoAntesDeHashOuEscritas() {
		assertThatThrownBy(() -> caso().executar(entrada("Fuso/Inexistente")))
				.isInstanceOfSatisfying(ExcecaoNegocio.class, erro -> assertThat(erro.campos().getFirst().campo()).isEqualTo("fuso"));
		verifyNoInteractions(usuarios, nutricionistas, organizacoes, encoder, ids);
	}
}
