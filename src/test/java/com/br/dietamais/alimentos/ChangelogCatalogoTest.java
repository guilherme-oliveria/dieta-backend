package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.yaml.YamlChangeLogParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class ChangelogCatalogoTest {
	@Test void liquibaseInterpretaOsTresCsvsTipadosSemExecutarNoBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			liquibase.Scope.child(java.util.Map.of(liquibase.Scope.Attr.resourceAccessor.name(),recursos),()->{
				var log=new YamlChangeLogParser().parse("db/changelog/changes/010-catalogo-base.yaml",new ChangeLogParameters(),recursos);
				// OfflineConnection não informa suporte a batch; a geração precisa da capacidade do Postgres.
				var banco=new liquibase.database.core.PostgresDatabase(){@Override public boolean supportsBatchUpdates(){return true;}};
				banco.setConnection(new liquibase.database.OfflineConnection("offline:postgresql?version=18",recursos));
				int cargas=0;
				for(var change:log.getChangeSets().getFirst().getChanges())if(change instanceof liquibase.change.core.LoadDataChange carga){
					int linhas=carga.getTableName().equals("alimento")?482:12;
					assertThat(carga.generateStatements(banco)).hasSize(linhas);cargas++;
				}
				assertThat(cargas).isEqualTo(3);
			});
		}
	}
	@Test void interpretaEstruturaSemExecutarSqlOuCargaIncompleta() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			var log=new YamlChangeLogParser().parse("db/changelog/changes/010-catalogo-base.yaml",new ChangeLogParameters(),recursos);
			assertThat(log.getChangeSets()).hasSize(1);assertThat(log.getChangeSets().getFirst().getId()).isEqualTo("010-catalogo-base");
			assertThat(log.getChangeSets().getFirst().getChanges()).isNotEmpty();
		}
	}
}
