package com.br.dietamais.pacientes.api;

import static org.assertj.core.api.Assertions.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FichaRequisicaoTest {
	@Test void validaLimitesDePersistenciaSemArredondarEstatura() {
		try(var fabrica=Validation.buildDefaultValidatorFactory()) {
			var validador=fabrica.getValidator();
			var invalida=new CriarPacienteRequisicao(null,"x".repeat(151),"invalido",null,null,"x".repeat(31),new BigDecimal("1.2345"),"x".repeat(2001),"x".repeat(10001));
			assertThat(validador.validate(invalida)).extracting(erro->erro.getPropertyPath().toString())
				.contains("nome","email","telefone","estaturaReferencia","objetivo","observacoes");
			assertThat(validador.validate(new AtualizarPacienteRequisicao("Fictício",null,null,null,null,new BigDecimal("1.725"),null,null))).isEmpty();
		}
	}
}
