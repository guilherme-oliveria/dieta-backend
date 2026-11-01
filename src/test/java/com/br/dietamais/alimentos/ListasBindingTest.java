package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.dominio.*;
import com.br.dietamais.alimentos.infra.*;
import java.sql.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ListasBindingTest {
	final UUID nid=UUID.randomUUID(),uid=UUID.randomUUID(),id=UUID.randomUUID(),grupo=UUID.randomUUID();
	final Instant agora=Instant.parse("2026-10-27T12:00:00.123456789Z");
	final DadosAlimento dados=new DadosAlimento(grupo,"Fictício","1 unidade",BigDecimal.TEN,true);
	@Test void criacaoConverteInstantesNaFronteiraJdbc() throws Exception {
		var f=fixture();assertThat(new ListasAlimentosStore(f.jdbc()).criar(id,nid,dados,agora)).isTrue();timestamp(f.statement(),8);timestamp(f.statement(),9);
	}
	@Test void edicaoDeAlimentoConverteInstante() throws Exception {
		var f=fixture();new ListasAlimentosStore(f.jdbc()).atualizar(id,nid,dados,agora);timestamp(f.statement(),6);
	}
	@Test void edicaoComMarcadoresGravaArrayEMantemOInstante() throws Exception {
		var f=fixture();new ListasAlimentosStore(f.jdbc()).atualizar(id,nid,new DadosAlimento(grupo,"Fictício","1 unidade",BigDecimal.TEN,true,List.of(MarcadorAlimento.CONTEM_LACTOSE,MarcadorAlimento.ALTO_SODIO,MarcadorAlimento.CONTEM_LACTOSE)),agora);
		verify(f.statement()).setString(6,"{CONTEM_LACTOSE,ALTO_SODIO}");timestamp(f.statement(),7);
	}
	@Test void diferencasUsamArraysTipadosETimestampSuportado() throws Exception {
		var f=fixture();new ListasAlimentosStore(f.jdbc()).atualizarAjuste(id,nid,new DadosAjusteLista(List.of(grupo),List.of()),agora);
		verify(f.statement()).setString(1,"{"+grupo+"}");verify(f.statement()).setString(2,"{}");timestamp(f.statement(),3);
	}
	@Test void macrosUsamClockETipoJdbcSemAlterarBase() throws Exception {
		var f=fixture();new CatalogoAlimentosStore(f.jdbc()).atualizar(grupo,nid,uid,new AtualizarMacrosGrupoRequisicao(BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ZERO),agora);
		timestamp(f.statement(),5);timestamp(f.statement(),6);
	}
	void timestamp(PreparedStatement statement,int indice) throws Exception {
		var argumento=org.mockito.ArgumentCaptor.forClass(Timestamp.class);verify(statement).setObject(eq(indice),argumento.capture());assertThat(argumento.getValue().toInstant()).isEqualTo(agora);
		verify(statement,never()).setObject(anyInt(),isA(Instant.class));
	}
	Fixture fixture() throws Exception {
		var ds=mock(DataSource.class);var connection=mock(Connection.class);var statement=mock(PreparedStatement.class);
		when(ds.getConnection()).thenReturn(connection);when(connection.prepareStatement(anyString())).thenReturn(statement);when(statement.executeUpdate()).thenReturn(1);
		doAnswer(a->{if(a.getArgument(1) instanceof Instant)throw new SQLDataException("Instant não suportado");return null;}).when(statement).setObject(anyInt(),any());
		return new Fixture(new JdbcTemplate(ds),statement);
	}
	record Fixture(JdbcTemplate jdbc,PreparedStatement statement){}
}
