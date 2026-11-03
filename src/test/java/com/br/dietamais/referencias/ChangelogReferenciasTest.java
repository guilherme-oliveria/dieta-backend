package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.yaml.YamlChangeLogParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class ChangelogReferenciasTest {
	@Test void liquibaseGeraUmaLinhaPorFaixaDoCsvSemExecutarNoBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			liquibase.Scope.child(java.util.Map.of(liquibase.Scope.Attr.resourceAccessor.name(),recursos),()->{
				var log=new YamlChangeLogParser().parse("db/changelog/changes/030-referencias.yaml",new ChangeLogParameters(),recursos);
				assertThat(log.getChangeSets()).hasSize(1);
				var banco=new liquibase.database.core.PostgresDatabase(){@Override public boolean supportsBatchUpdates(){return true;}};
				banco.setConnection(new liquibase.database.OfflineConnection("offline:postgresql?version=18",recursos));
				int cargas=0;
				for(var change:log.getChangeSets().getFirst().getChanges())if(change instanceof liquibase.change.core.LoadDataChange carga){
					assertThat(carga.generateStatements(banco)).hasSize(118);cargas++;
				}
				assertThat(cargas).isEqualTo(1);
			});
		}
	}

	@Test void changesetDaTacoCarregaOs597AlimentosSemExecutarNoBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			liquibase.Scope.child(java.util.Map.of(liquibase.Scope.Attr.resourceAccessor.name(),recursos),()->{
				var log=new YamlChangeLogParser().parse("db/changelog/changes/036-referencia-taco.yaml",new ChangeLogParameters(),recursos);
				assertThat(log.getChangeSets()).hasSize(1);
				var banco=new liquibase.database.core.PostgresDatabase(){@Override public boolean supportsBatchUpdates(){return true;}};
				banco.setConnection(new liquibase.database.OfflineConnection("offline:postgresql?version=18",recursos));
				int cargas=0;
				for(var change:log.getChangeSets().getFirst().getChanges())if(change instanceof liquibase.change.core.LoadDataChange carga){
					assertThat(carga.generateStatements(banco)).hasSize(597);cargas++;
				}
				assertThat(cargas).isEqualTo(1);
			});
		}
	}

	@Test void changesetDaOmsCarrega736LinhasSemExecutarNoBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			liquibase.Scope.child(java.util.Map.of(liquibase.Scope.Attr.resourceAccessor.name(),recursos),()->{
				var log=new YamlChangeLogParser().parse("db/changelog/changes/037-referencia-oms.yaml",new ChangeLogParameters(),recursos);
				assertThat(log.getChangeSets()).hasSize(1);
				var banco=new liquibase.database.core.PostgresDatabase(){@Override public boolean supportsBatchUpdates(){return true;}};
				banco.setConnection(new liquibase.database.OfflineConnection("offline:postgresql?version=18",recursos));
				int cargas=0;
				for(var change:log.getChangeSets().getFirst().getChanges())if(change instanceof liquibase.change.core.LoadDataChange carga){
					assertThat(carga.generateStatements(banco)).hasSize(736);cargas++;
				}
				assertThat(cargas).isEqualTo(1);
			});
		}
	}

	@Test void changesetDosExamesCarregaCatalogoERegrasSemExecutarNoBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			liquibase.Scope.child(java.util.Map.of(liquibase.Scope.Attr.resourceAccessor.name(),recursos),()->{
				var log=new YamlChangeLogParser().parse("db/changelog/changes/038-referencia-exame.yaml",new ChangeLogParameters(),recursos);
				assertThat(log.getChangeSets()).hasSize(1);
				var banco=new liquibase.database.core.PostgresDatabase(){@Override public boolean supportsBatchUpdates(){return true;}};
				banco.setConnection(new liquibase.database.OfflineConnection("offline:postgresql?version=18",recursos));
				var tamanhos=new java.util.ArrayList<Integer>();
				for(var change:log.getChangeSets().getFirst().getChanges())if(change instanceof liquibase.change.core.LoadDataChange carga)tamanhos.add(carga.generateStatements(banco).length);
				assertThat(tamanhos).containsExactly(395,2223);
			});
		}
	}

	@Test void changesetDeFrisanchoCarrega80LinhasSemExecutarNoBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			liquibase.Scope.child(java.util.Map.of(liquibase.Scope.Attr.resourceAccessor.name(),recursos),()->{
				var log=new YamlChangeLogParser().parse("db/changelog/changes/039-referencia-frisancho.yaml",new ChangeLogParameters(),recursos);
				assertThat(log.getChangeSets()).hasSize(1);
				var banco=new liquibase.database.core.PostgresDatabase(){@Override public boolean supportsBatchUpdates(){return true;}};
				banco.setConnection(new liquibase.database.OfflineConnection("offline:postgresql?version=18",recursos));
				int cargas=0;
				for(var change:log.getChangeSets().getFirst().getChanges())if(change instanceof liquibase.change.core.LoadDataChange carga){
					assertThat(carga.generateStatements(banco)).hasSize(80);cargas++;
				}
				assertThat(cargas).isEqualTo(1);
			});
		}
	}

	@Test void changesetDePorcoesCarregaGruposPorcoesENutrientesSemExecutarNoBanco() throws Exception {
		try(var recursos=new ClassLoaderResourceAccessor()){
			liquibase.Scope.child(java.util.Map.of(liquibase.Scope.Attr.resourceAccessor.name(),recursos),()->{
				var log=new YamlChangeLogParser().parse("db/changelog/changes/040-porcoes-grupos.yaml",new ChangeLogParameters(),recursos);
				assertThat(log.getChangeSets()).hasSize(1);
				var banco=new liquibase.database.core.PostgresDatabase(){@Override public boolean supportsBatchUpdates(){return true;}};
				banco.setConnection(new liquibase.database.OfflineConnection("offline:postgresql?version=18",recursos));
				var contagens=new java.util.ArrayList<Integer>();
				for(var change:log.getChangeSets().getFirst().getChanges())if(change instanceof liquibase.change.core.LoadDataChange carga)contagens.add(carga.generateStatements(banco).length);
				assertThat(contagens).containsExactly(3,31,1054);
			});
		}
	}
}
