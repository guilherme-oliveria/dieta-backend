package com.br.dietamais.relatorios.aplicacao;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoscaPdfTest {
	@Test void roscaGeraPngValido(){
		var png=PdfAvaliacao.rosca(List.of(27.15,43.0,5.0,24.7));
		assertThat(png).isNotNull();assertThat(new String(png,1,3)).isEqualTo("PNG");
	}
}
