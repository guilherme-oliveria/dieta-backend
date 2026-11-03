package com.br.dietamais.pacientes.dominio;

import com.br.dietamais.pacientes.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record DadosFicha(String nome,String email,LocalDate dataNascimento,Sexo sexo,String telefone,
		BigDecimal estaturaReferencia,String objetivo,String observacoes) {
	public DadosFicha {
		nome=nome.strip();email=limpar(email);if(email!=null)email=email.toLowerCase(Locale.ROOT);
		telefone=limpar(telefone);objetivo=limpar(objetivo);observacoes=limpar(observacoes);
	}
	public static DadosFicha de(CriarPacienteRequisicao r){return new DadosFicha(r.nome(),r.email(),r.dataNascimento(),r.sexo(),r.telefone(),r.estaturaReferencia(),r.objetivo(),r.observacoes());}
	public static DadosFicha de(AtualizarPacienteRequisicao r){return new DadosFicha(r.nome(),r.email(),r.dataNascimento(),r.sexo(),r.telefone(),r.estaturaReferencia(),r.objetivo(),r.observacoes());}
	private static String limpar(String valor){return valor==null||valor.isBlank()?null:valor.strip();}
}
