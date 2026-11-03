package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.dominio.PoliticaSenha;
import com.br.dietamais.contas.infra.SessaoStore;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(propagation=Propagation.MANDATORY)
public class ServicoContasPaciente implements ContasPaciente {
	private final JdbcTemplate jdbc;private final SessaoStore contas;private final PoliticaSenha politica;
	private final PasswordEncoder encoder;private final GeradorIds ids;private final Clock relogio;private final AutenticarSessao sessoes;
	private final EmitirTokenConta emissor;private final String fusoPadrao;
	private final TermosUso termos;
	public ServicoContasPaciente(JdbcTemplate jdbc,SessaoStore contas,PoliticaSenha politica,PasswordEncoder encoder,GeradorIds ids,Clock relogio,AutenticarSessao sessoes,EmitirTokenConta emissor,
			@org.springframework.beans.factory.annotation.Value("${dieta.contas.fuso-padrao:America/Sao_Paulo}") String fusoPadrao,TermosUso termos) {
		this.jdbc=jdbc;this.contas=contas;this.politica=politica;this.encoder=encoder;this.ids=ids;this.relogio=relogio;this.sessoes=sessoes;
		this.emissor=emissor;this.fusoPadrao=ZoneId.of(fusoPadrao).getId();
		this.termos=termos;
	}
	@Override public UUID criar(String nome,String email,String senha,String fuso,Boolean aceitouTermos,String versaoTermos) {
		termos.validar(aceitouTermos,versaoTermos);
		politica.validar(senha);
		try {fuso=fuso==null||fuso.isBlank()?fusoPadrao:ZoneId.of(fuso).getId();}
		catch(DateTimeException erro){throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);}
		var id=ids.novo();var agora=Timestamp.from(relogio.instant());
		int criado=jdbc.update("""
			insert into usuario(id,email,senha_hash,nome,perfil,ativo,criado_em,atualizado_em,fuso,termos_aceitos_em,versao_termos)
			values(?,?,?,?,'PACIENTE',true,?,?,?,?,?) on conflict do nothing
			""",id,email,encoder.encode(senha),nome,agora,agora,fuso,agora,versaoTermos);
		if(criado==0)throw new ExcecaoNegocio(CodigoErro.EMAIL_JA_CADASTRADO,"RN-CON-01");
		emissor.emitir(id,email,com.br.dietamais.contas.dominio.FinalidadeToken.CONFIRMAR_EMAIL);
		return id;
	}
	@Override public UsuarioResposta bloquear(UUID usuario) {
		if(!contas.bloquearConta(usuario))throw new ExcecaoNegocio(CodigoErro.NAO_AUTENTICADO,null);
		var conta=contas.consultarConta(usuario);
		if(conta==null||!conta.ativo())throw new ExcecaoNegocio(CodigoErro.NAO_AUTENTICADO,null);
		if(conta.legado()==Perfil.ADMIN)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		return conta.resposta(null);
	}
	@Override public SessaoResposta emitir(UUID usuario,DispositivoRequisicao dispositivo){return sessoes.emitirPaciente(usuario,dispositivo);}
}
