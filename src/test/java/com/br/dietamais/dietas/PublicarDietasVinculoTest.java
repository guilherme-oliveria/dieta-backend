package com.br.dietamais.dietas;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.aplicacao.*;
import com.br.dietamais.dietas.dominio.LinhasDieta.Dieta;
import com.br.dietamais.dietas.infra.DietaStore;
import com.br.dietamais.pacientes.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import tools.jackson.databind.json.JsonMapper;

class PublicarDietasVinculoTest {
	final MontadorDieta montador=mock(MontadorDieta.class);final DietaStore dietas=mock(DietaStore.class);
	final ConsultarAlimentosParaDieta alimentos=mock(ConsultarAlimentosParaDieta.class);final ListaSubstituicaoEfetiva listas=mock(ListaSubstituicaoEfetiva.class);
	final ConsultaNutricionistas nutricionistas=mock(ConsultaNutricionistas.class);final ConsultarFichaParaLista fichas=mock(ConsultarFichaParaLista.class);
	final UsuarioAtual usuario=mock(UsuarioAtual.class);final GuardaEscritaNutri guarda=mock(GuardaEscritaNutri.class);final AuditoriaClinica auditoria=mock(AuditoriaClinica.class);final GeradorIds ids=mock(GeradorIds.class);final ApplicationEventPublisher eventos=mock(ApplicationEventPublisher.class);
	final UUID id=UUID.randomUUID(),nutri=UUID.randomUUID(),paciente=UUID.randomUUID(),antigo=UUID.randomUUID(),novo=UUID.randomUUID(),gerado=UUID.randomUUID(),food=UUID.randomUUID();
	final Instant agora=Instant.parse("2026-10-28T08:00:00Z");
	final Dieta origem=new Dieta(id,antigo,paciente,"Fictício",null,StatusDieta.RASCUNHO,1L,agora,agora);
	final Dieta copia=new Dieta(gerado,antigo,paciente,"Fictício (cópia)",null,StatusDieta.RASCUNHO,1L,agora,agora);
	final PublicarDietas caso=new PublicarDietas(montador,dietas,alimentos,listas,nutricionistas,fichas,usuario,guarda,auditoria,ids,Clock.fixed(agora,ZoneOffset.UTC),JsonMapper.builder().build(),eventos);
	@BeforeEach void configurar(){
		when(usuario.obter()).thenReturn(new ContextoUsuario(new UsuarioResposta(UUID.randomUUID(),"Fictício","ficticio@example.invalid",true,List.of(Perfil.NUTRICIONISTA),Perfil.NUTRICIONISTA,nutri,null,"UTC"),UUID.randomUUID()));
		when(dietas.dieta(id,nutri,true)).thenReturn(origem);when(dietas.dieta(id,nutri,false)).thenReturn(origem);when(dietas.dieta(gerado,nutri,false)).thenReturn(copia);when(ids.novo()).thenReturn(gerado);
		var totais=new TotaisNutricionaisResposta(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE);
		var item=new ItemRefeicaoResposta(UUID.randomUUID(),food,UUID.randomUUID(),"Fictício",BigDecimal.ONE,"1 unidade",BigDecimal.ONE,null,totais,null);
		var refeicao=new RefeicaoResposta(UUID.randomUUID(),"Fictícia",LocalTime.NOON,0,List.of(item),totais);
		when(montador.montar(List.of(origem),nutri)).thenReturn(List.of(new DietaResposta(id,paciente,antigo,"Fictício",StatusDieta.RASCUNHO,null,List.of(refeicao),totais,1L,agora,agora)));
		when(montador.montar(List.of(copia),nutri)).thenReturn(List.of(new DietaResposta(gerado,paciente,antigo,"Fictício (cópia)",StatusDieta.RASCUNHO,null,List.of(),totais,1L,agora,agora)));
		when(nutricionistas.porIds(Set.of(nutri))).thenReturn(Map.of(nutri,new ConsultaNutricionistas.DadosNutri(nutri,"Nutri fictícia","CRN-1 12345",UUID.randomUUID())));
		when(listas.consultar(antigo,paciente,nutri)).thenReturn(new ListaSubstituicaoResposta(paciente,1L,List.of(),List.of(),List.of()));
	}
	@Test void novoVinculoAtivoNaoAutorizaPublicarDietaDoAntigoEncerrado(){
		when(fichas.consultar(paciente,true)).thenReturn(new FichaParaLista(novo,paciente,StatusVinculo.ATIVO));negado(()->caso.publicar(id,"\"1\""));semEscrita();verifyNoInteractions(montador,listas,nutricionistas,alimentos);
	}
	@Test void novoVinculoAtivoNaoAutorizaDuplicarDietaDoAntigoEncerrado(){
		when(fichas.consultar(paciente,true)).thenReturn(new FichaParaLista(novo,paciente,StatusVinculo.ATIVO));negado(()->caso.duplicar(id));semEscrita();verifyNoInteractions(ids);
	}
	@Test void mesmoVinculoEncerradoContinuaBloqueandoAsDuasOperacoes(){
		when(fichas.consultar(paciente,true)).thenReturn(new FichaParaLista(antigo,paciente,StatusVinculo.ENCERRADO));negado(()->caso.publicar(id,"\"1\""));negado(()->caso.duplicar(id));semEscrita();
	}
	@Test void mesmoVinculoAtivoContinuaPermitindoPublicarEDuplicar(){
		when(fichas.consultar(paciente,true)).thenReturn(new FichaParaLista(antigo,paciente,StatusVinculo.ATIVO));
		assertThat(caso.publicar(id,"\"1\"").vinculoId()).isEqualTo(antigo);verify(dietas).marcarPublicada(id,nutri,agora);verify(eventos).publishEvent(isA(DietaPublicada.class));
		assertThat(caso.duplicar(id).vinculoId()).isEqualTo(antigo);verify(dietas).inserir(gerado,antigo,nutri,paciente,"Fictício (cópia)",null,new DietaStore.Extras(null,null,null,null,java.util.List.of()),agora);
	}
	void negado(Runnable acao){assertThatThrownBy(acao::run).isInstanceOfSatisfying(ExcecaoNegocio.class,e->{assertThat(e.codigo()).isEqualTo(CodigoErro.ACESSO_NEGADO);assertThat(e.regra()).isEqualTo("RN-VIN-05");});}
	void semEscrita(){verify(dietas,never()).marcarPublicada(any(),any(),any());verify(dietas,never()).inserirSnapshot(any(),any(),any(),any(),any(),any(),any());verify(dietas,never()).inserir(any(),any(),any(),any(),any(),any(),any(),any());verifyNoInteractions(eventos);verify(auditoria,never()).escrita(any());}
}
