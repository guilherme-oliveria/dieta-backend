package com.br.dietamais.sync;

import static org.assertj.core.api.Assertions.*;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.yaml.YamlChangeLogParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class ChangelogSyncTest {
	@Test void liquibaseConsegueInterpretarNovaMigracaoSemBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()) {
			var changelog=new YamlChangeLogParser().parse("db/changelog/changes/009-sync-leitura.yaml",new ChangeLogParameters(),recursos);
			assertThat(changelog.getChangeSets()).hasSize(1);assertThat(changelog.getChangeSets().getFirst().getId()).isEqualTo("009-diario-sync");
			assertThat(changelog.getChangeSets().getFirst().getChanges()).isNotEmpty();
		}
	}
}
