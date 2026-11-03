package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.Armazenamento;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Com {@code S3_ENDPOINT} usa S3/MinIO; sem ele, disco local (dev e beta). Segredos só em variável de ambiente. */
@Configuration
class ArmazenamentoConfig {
	@Bean Armazenamento armazenamento(Clock relogio,
			@Value("${dieta.armazenamento.s3.endpoint:}") String endpoint,@Value("${dieta.armazenamento.s3.bucket:dietamais}") String bucket,
			@Value("${dieta.armazenamento.s3.regiao:us-east-1}") String regiao,@Value("${dieta.armazenamento.s3.chave-acesso:}") String acesso,
			@Value("${dieta.armazenamento.s3.chave-secreta:}") String segredo,
			@Value("${dieta.armazenamento.local.dir:}") String dir,@Value("${dieta.armazenamento.local.segredo:}") String segredoLocal,
			@Value("${dieta.publico.url-base:http://localhost:8080}") String urlBase) {
		if(!endpoint.isBlank()){
			if(acesso.isBlank()||segredo.isBlank())throw new IllegalStateException("S3_CHAVE_ACESSO e S3_CHAVE_SECRETA são obrigatórias com S3_ENDPOINT.");
			return new ArmazenamentoS3(endpoint,bucket,regiao,acesso,segredo,relogio);
		}
		byte[] chave=segredoLocal.isBlank()?aleatorio():segredoLocal.getBytes(StandardCharsets.UTF_8);
		var raiz=dir.isBlank()?Path.of(System.getProperty("java.io.tmpdir"),"dietamais-arquivos"):Path.of(dir);
		return new ArmazenamentoLocal(raiz,chave,urlBase,relogio);
	}
	private static byte[] aleatorio(){var b=new byte[32];new SecureRandom().nextBytes(b);return b;}
}
