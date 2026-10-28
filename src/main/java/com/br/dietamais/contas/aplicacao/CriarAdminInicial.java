package com.br.dietamais.contas.aplicacao;

import java.sql.Timestamp;
import java.time.Clock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cria o admin da plataforma na subida quando {@code ADMIN_EMAIL} e {@code ADMIN_SENHA} estão definidos e a conta ainda não existe.
 * A senha vem só de variável de ambiente e nunca vai para o log; troque-a depois pelo fluxo de redefinição.
 */
@Component
public class CriarAdminInicial implements ApplicationRunner {
	private final JdbcTemplate jdbc;private final PasswordEncoder encoder;private final TransactionTemplate tx;private final Clock relogio;
	private final String email;private final String senha;
	public CriarAdminInicial(JdbcTemplate jdbc,PasswordEncoder encoder,TransactionTemplate tx,Clock relogio,
			@Value("${dieta.admin.email:}") String email,@Value("${dieta.admin.senha:}") String senha){
		this.jdbc=jdbc;this.encoder=encoder;this.tx=tx;this.relogio=relogio;this.email=email.strip().toLowerCase();this.senha=senha;
	}
	@Override public void run(ApplicationArguments args){
		if(email.isBlank()||senha.isBlank())return;
		if(senha.length()<12)throw new IllegalStateException("ADMIN_SENHA precisa ter ao menos 12 caracteres.");
		tx.executeWithoutResult(s->{
			if(jdbc.queryForObject("select exists(select 1 from usuario where lower(btrim(email))=?)",Boolean.class,email))return;
			var agora=Timestamp.from(relogio.instant());
			jdbc.update("insert into usuario(email,senha_hash,nome,perfil,ativo,email_confirmado_em,criado_em,atualizado_em) values(?,?,'Administração Dieta+','ADMIN',true,?,?,?)",email,encoder.encode(senha),agora,agora,agora);
			LoggerFactory.getLogger(CriarAdminInicial.class).info("Conta de administração criada");
		});
	}
}
