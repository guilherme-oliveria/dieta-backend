package com.br.dietamais.contas.aplicacao;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import com.br.dietamais.assinaturas.api.CriarOrganizacaoIndividual;
import com.br.dietamais.comum.api.CodigoErro;
import com.br.dietamais.comum.api.ErroCampoResposta;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import com.br.dietamais.comum.api.GeradorIds;
import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.comum.api.UsuarioResposta;
import com.br.dietamais.contas.api.CadastroNutriRequisicao;
import com.br.dietamais.contas.api.CadastroNutriResposta;
import com.br.dietamais.contas.dominio.Nutricionista;
import com.br.dietamais.contas.dominio.PoliticaSenha;
import com.br.dietamais.contas.infra.NutricionistaRepository;
import com.br.dietamais.contas.infra.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarNutri {
	private final UsuarioRepository usuarios;
	private final NutricionistaRepository nutricionistas;
	private final CriarOrganizacaoIndividual criarOrganizacao;
	private final PoliticaSenha politicaSenha;
	private final PasswordEncoder encoder;
	private final GeradorIds ids;
	private final Clock relogio;
	private final String fusoPadrao;

	public CadastrarNutri(UsuarioRepository usuarios, NutricionistaRepository nutricionistas,
			CriarOrganizacaoIndividual criarOrganizacao, PoliticaSenha politicaSenha, PasswordEncoder encoder,
			GeradorIds ids, Clock relogio, @Value("${dieta.contas.fuso-padrao:America/Sao_Paulo}") String fusoPadrao) {
		this.usuarios = usuarios;
		this.nutricionistas = nutricionistas;
		this.criarOrganizacao = criarOrganizacao;
		this.politicaSenha = politicaSenha;
		this.encoder = encoder;
		this.ids = ids;
		this.relogio = relogio;
		this.fusoPadrao = ZoneId.of(fusoPadrao).getId();
	}

	@Transactional
	public CadastroNutriResposta executar(CadastroNutriRequisicao entrada) {
		politicaSenha.validar(entrada.senha());
		String fuso = validarFuso(entrada.fuso());
		var agora = relogio.instant();
		var usuarioId = ids.novo();
		String hash = encoder.encode(entrada.senha());
		if (usuarios.inserirSeEmailDisponivel(usuarioId, entrada.email(), hash, entrada.nome(), agora, fuso, entrada.versaoTermos()) == 0) {
			throw new ExcecaoNegocio(CodigoErro.EMAIL_JA_CADASTRADO, "RN-CON-01");
		}
		var assinatura = criarOrganizacao.criar(entrada.nome(), agora);
		var nutri = nutricionistas.saveAndFlush(new Nutricionista(ids.novo(), usuarioId, assinatura.organizacaoId(), entrada.crn()));
		var usuario = new UsuarioResposta(usuarioId, entrada.nome(), entrada.email(), false, List.of(Perfil.NUTRICIONISTA),
				Perfil.NUTRICIONISTA, nutri.getId(), null, fuso);
		return new CadastroNutriResposta(usuario, assinatura);
	}

	private String validarFuso(String fuso) {
		if (fuso == null || fuso.isBlank()) return fusoPadrao;
		try { return ZoneId.of(fuso).getId(); }
		catch (DateTimeException erro) {
			throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO, null, "Informe um fuso válido.",
					List.of(new ErroCampoResposta("fuso", "FORMATO_INVALIDO", "Informe um fuso válido.")), null);
		}
	}
}
