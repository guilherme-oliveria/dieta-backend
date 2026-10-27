package com.br.dietamais.contas.dominio;

import static org.assertj.core.api.Assertions.*;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.yaml.YamlChangeLogParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class ChangelogPerfilNutriTest {
	@Test void interpretaMigracaoNovaSemAlterarHistoricas() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			var log=new YamlChangeLogParser().parse("db/changelog/changes/011-perfil-nutri.yaml",new ChangeLogParameters(),recursos);
			assertThat(log.getChangeSets()).hasSize(1);assertThat(log.getChangeSets().getFirst().getId()).isEqualTo("011-perfil-nutri");
			assertThat(log.getChangeSets().getFirst().getChanges()).isNotEmpty();
		}
	}
}
