package com.br.dietamais.contas.infra;

import static org.mockito.Mockito.*;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import com.br.dietamais.contas.dominio.DadosPerfilNutri;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PerfilNutriStoreTest {
	@Test void jdbcRealLigaInstanteComoTimestampPreservandoPrecisao() throws Exception {
		var dataSource=mock(DataSource.class);var conexao=mock(Connection.class);var statement=mock(PreparedStatement.class);
		when(dataSource.getConnection()).thenReturn(conexao);when(conexao.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeUpdate()).thenReturn(1);
		// A fronteira simula a restrição do driver, sem mockar JdbcTemplate ou o store.
		doAnswer(invocacao->{if(invocacao.getArgument(1) instanceof Instant)throw new SQLDataException("Instant não suportado em setObject");return null;})
			.when(statement).setObject(anyInt(),any());
		var instante=Instant.parse("2026-10-27T02:00:00.123456789Z");var nutri=UUID.randomUUID();var usuario=UUID.randomUUID();
		new PerfilNutriStore(new JdbcTemplate(dataSource)).atualizar(nutri,usuario,new DadosPerfilNutri("Fictício","CRN-1 00000","123","Texto fictício"),instante);
		var timestamp=org.mockito.ArgumentCaptor.forClass(Timestamp.class);
		verify(statement).setObject(eq(6),timestamp.capture());
		org.assertj.core.api.Assertions.assertThat(timestamp.getValue().toInstant()).isEqualTo(instante);
		verify(statement,never()).setObject(anyInt(),isA(Instant.class));
		verify(statement).executeUpdate();
	}
}
