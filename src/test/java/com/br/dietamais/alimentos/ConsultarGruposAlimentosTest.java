package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import com.br.dietamais.alimentos.aplicacao.ConsultarGruposAlimentos;
import com.br.dietamais.alimentos.infra.CatalogoAlimentosStore;
import com.br.dietamais.comum.api.*;
import org.junit.jupiter.api.Test;

class ConsultarGruposAlimentosTest {
	final UsuarioAtual usuario=mock(UsuarioAtual.class);
	final CatalogoAlimentosStore catalogo=mock(CatalogoAlimentosStore.class);
	final ConsultarGruposAlimentos caso=new ConsultarGruposAlimentos(usuario,catalogo);
	@Test void catalogoGlobalNaoConsultaDadosClinicosDeUmDono() {
		var contexto=contexto(Perfil.NUTRICIONISTA);when(usuario.obter()).thenReturn(contexto);when(catalogo.grupos(contexto.exigirNutricionista())).thenReturn(List.of());
		assertThat(caso.executar().itens()).isEmpty();verify(catalogo).grupos(contexto.exigirNutricionista());
	}
	@Test void pacienteEAdminNaoCarregamCatalogoPrivilegiado() {
		for(var perfil:List.of(Perfil.PACIENTE,Perfil.ADMIN)){
			when(usuario.obter()).thenReturn(contexto(perfil));
			assertThatThrownBy(caso::executar).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.PERFIL_INVALIDO));
		}
		verifyNoInteractions(catalogo);
	}
	ContextoUsuario contexto(Perfil perfil){return new ContextoUsuario(new UsuarioResposta(UUID.randomUUID(),"Fictício","ficticio@example.invalid",true,List.of(perfil),perfil,UUID.randomUUID(),UUID.randomUUID(),"UTC"),UUID.randomUUID());}
}
