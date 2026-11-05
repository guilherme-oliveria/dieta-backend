package com.br.dietamais.pacientes.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Cadastro ampliado do paciente (BE-64): campos da aba Cadastro da planilha, todos opcionais e em texto livre curto (o app sugere opções, o servidor não as fixa).
 * Dado pessoal do paciente: nunca vai para log, e só a nutri dona da ficha o lê.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CadastroAmpliado(
		@Schema(nullable = true) @Size(max=60) String escolaridade,
		@Schema(nullable = true) @Size(max=40) String estadoCivil,
		@Schema(nullable = true) @Size(max=100) String profissao,
		@Schema(nullable = true) @Valid Endereco endereco,
		@Schema(nullable = true) @Size(max=100) String convenio,
		@Schema(nullable = true) @Size(max=150) String indicacao,
		@Schema(nullable = true) @Size(max=2000) String motivoConsulta) {
	public CadastroAmpliado {
		escolaridade=limpar(escolaridade);estadoCivil=limpar(estadoCivil);profissao=limpar(profissao);convenio=limpar(convenio);indicacao=limpar(indicacao);motivoConsulta=limpar(motivoConsulta);
		if(endereco!=null&&endereco.vazio())endereco=null;
	}
	/** Sem nenhum campo preenchido. */
	@com.fasterxml.jackson.annotation.JsonIgnore
	public boolean vazio(){return escolaridade==null&&estadoCivil==null&&profissao==null&&endereco==null&&convenio==null&&indicacao==null&&motivoConsulta==null;}
	/** Endereço do paciente; `cep` só com os 8 dígitos e `uf` em maiúsculas. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Endereco(
			@Schema(nullable = true) @Size(max=150) String logradouro,
			@Schema(nullable = true) @Size(max=20) String numero,
			@Schema(nullable = true) @Size(max=80) String complemento,
			@Schema(nullable = true) @Size(max=80) String bairro,
			@Schema(nullable = true) @Size(max=80) String cidade,
			@Schema(nullable = true) @Pattern(regexp="^[A-Za-z]{2}$") String uf,
			@Schema(nullable = true) @Pattern(regexp="^\\d{5}-?\\d{3}$") String cep) {
		public Endereco {
			logradouro=limpar(logradouro);numero=limpar(numero);complemento=limpar(complemento);bairro=limpar(bairro);cidade=limpar(cidade);
			uf=limpar(uf);if(uf!=null)uf=uf.toUpperCase(Locale.ROOT);
			cep=limpar(cep);if(cep!=null&&cep.matches("^\\d{5}-?\\d{3}$"))cep=cep.replace("-","");
		}
		@com.fasterxml.jackson.annotation.JsonIgnore
		public boolean vazio(){return logradouro==null&&numero==null&&complemento==null&&bairro==null&&cidade==null&&uf==null&&cep==null;}
	}
	private static String limpar(String valor){return valor==null||valor.isBlank()?null:valor.strip();}
}
