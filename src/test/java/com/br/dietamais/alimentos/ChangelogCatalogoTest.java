package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.yaml.YamlChangeLogParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class ChangelogCatalogoTest {
	@Test void interpretaEstruturaSemExecutarSqlOuCargaIncompleta() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			var log=new YamlChangeLogParser().parse("db/changelog/changes/010-catalogo-base.yaml",new ChangeLogParameters(),recursos);
			assertThat(log.getChangeSets()).hasSize(1);assertThat(log.getChangeSets().getFirst().getId()).isEqualTo("010-catalogo-base");
			assertThat(log.getChangeSets().getFirst().getChanges()).isNotEmpty();
		}
	}
}
