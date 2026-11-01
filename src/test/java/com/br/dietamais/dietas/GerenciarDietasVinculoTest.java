package com.br.dietamais.dietas;

import com.br.dietamais.alimentos.api.ConsultarAlimentosParaDieta;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.aplicacao.*;
import com.br.dietamais.dietas.dominio.LinhasDieta.Dieta;
import com.br.dietamais.dietas.infra.DietaStore;
import com.br.dietamais.pacientes.api.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import tools.jackson.databind.json.JsonMapper;

class GerenciarDietasVinculoTest {
	final MontadorDieta montador=mock(MontadorDieta.class);final DietaStore dietas=mock(DietaStore.class);
	final ConsultarAlimentosParaDieta alimentos=mock(ConsultarAlimentosParaDieta.class);final ConsultarFichaParaLista fichas=mock(ConsultarFichaParaLista.class);
	final UsuarioAtual usuario=mock(UsuarioAtual.class);final GuardaEscritaNutri guarda=mock(GuardaEscritaNutri.class);final AuditoriaClinica auditoria=mock(AuditoriaClinica.class);
	final UUID id=UUID.randomUUID(),nutri=UUID.randomUUID(),paciente=UUID.randomUUID(),antigo=UUID.randomUUID(),novo=UUID.randomUUID();
	final Instant agora=Instant.parse("2026-10-28T07:00:00Z");
	final Dieta dieta=new Dieta(id,antigo,paciente,"Fictício",null,StatusDieta.RASCUNHO,1L,agora,agora);
	final SalvarDietaRequisicao entrada=new SalvarDietaRequisicao(null,"Fictício",null,List.of());
	final com.br.dietamais.referencias.api.ConsultaPorcoes porcoes=mock(com.br.dietamais.referencias.api.ConsultaPorcoes.class);
	final GerenciarDietas caso=new GerenciarDietas(porcoes,montador,dietas,alimentos,fichas,usuario,guarda,auditoria,mock(GeradorIds.class),Clock.fixed(agora,ZoneOffset.UTC),JsonMapper.builder().build());
	@BeforeEach void configurar(){
		when(porcoes.porcoes()).thenReturn(new com.br.dietamais.referencias.api.PorcoesGrupos("fonte",java.util.Map.of(),List.of()));
		when(usuario.obter()).thenReturn(new ContextoUsuario(new UsuarioResposta(UUID.randomUUID(),"Fictício","ficticio@example.invalid",true,List.of(Perfil.NUTRICIONISTA),Perfil.NUTRICIONISTA,nutri,null,"UTC"),UUID.randomUUID()));
		when(dietas.dieta(id,nutri,true)).thenReturn(dieta);when(dietas.dieta(id,nutri,false)).thenReturn(dieta);
		when(montador.montar(List.of(dieta),nutri)).thenReturn(List.of(new DietaResposta(id,paciente,antigo,"Fictício",StatusDieta.RASCUNHO,null,List.of(),new TotaisNutricionaisResposta(java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO),1L,agora,agora)));
	}
	@Test void novoVinculoAtivoNaoReabreEscritaDaDietaDoVinculoAntigoEncerrado(){
		when(fichas.consultar(paciente,true)).thenReturn(new FichaParaLista(novo,paciente,StatusVinculo.ATIVO));
		assertThatThrownBy(()->caso.atualizar(id,"\"1\"",entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->{assertThat(e.codigo()).isEqualTo(CodigoErro.ACESSO_NEGADO);assertThat(e.regra()).isEqualTo("RN-VIN-05");});
		verify(dietas,never()).limparConteudo(any());verify(dietas,never()).atualizar(any(),any(),any(),any(),any(),any());verifyNoInteractions(alimentos);verify(auditoria,never()).escrita(any());
		assertThat(caso.consultar(id).vinculoId()).isEqualTo(antigo);verify(auditoria).leitura(any());
	}
	@Test void mesmoVinculoAtivoContinuaEditavel(){
		when(fichas.consultar(paciente,true)).thenReturn(new FichaParaLista(antigo,paciente,StatusVinculo.ATIVO));
		caso.atualizar(id,"\"1\"",entrada);verify(dietas).limparConteudo(id);verify(dietas).atualizar(eq(id),eq(nutri),eq("Fictício"),isNull(),any(),eq(agora));verify(auditoria).escrita(any());
	}
	@Test void mesmoVinculoEncerradoContinuaSomenteLeitura(){
		when(fichas.consultar(paciente,true)).thenReturn(new FichaParaLista(antigo,paciente,StatusVinculo.ENCERRADO));
		assertThatThrownBy(()->caso.atualizar(id,"\"1\"",entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.ACESSO_NEGADO));verify(dietas,never()).limparConteudo(any());
	}
}
