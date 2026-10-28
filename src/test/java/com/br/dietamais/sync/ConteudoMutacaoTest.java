package com.br.dietamais.sync;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.sync.dominio.ConteudoMutacao;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;

class ConteudoMutacaoTest {
	final JsonMapper mapper=JsonMapper.builder().build();final UUID chave=UUID.randomUUID(),id=UUID.randomUUID();
	MutacaoSync mutacao(String dados){return new MutacaoSync(chave,"avaliacoes",OperacaoSync.ATUALIZAR,id,1L,mapper.readTree(dados));}
	byte[] bytes(MutacaoSync m){return ConteudoMutacao.canonico(m,mapper);}
	@Test void ordemDasPropriedadesEspacosEEscalaNumericaNaoAlteramConteudo(){
		assertThat(bytes(mutacao("{\"b\":{\"x\":1.00,\"a\":null},\"a\":[2,3]}"))).isEqualTo(bytes(mutacao("{ \"a\": [2.0,3], \"b\": {\"a\": null, \"x\": 1} }")));
	}
	@Test void normalizaNotacaoCientificaSemExpandirExpoentes(){
		assertThat(bytes(mutacao("{\"valor\":100}"))).isEqualTo(bytes(mutacao("{\"valor\":1e2}")));
		var dados=mapper.createObjectNode();dados.set("valor",mapper.getNodeFactory().numberNode(new java.math.BigDecimal("1E+100000")));assertThat(bytes(new MutacaoSync(chave,"avaliacoes",OperacaoSync.ATUALIZAR,id,1L,dados)).length).isLessThan(400);
	}
	@Test void todosOsCamposDaMutacaoEOrdemDosArraysSaoAutenticados(){
		var m=mutacao("{\"itens\":[1,2]}");var original=bytes(m);
		for(var outro:List.of(new MutacaoSync(chave,"agua",m.operacao(),id,1L,m.dados()),new MutacaoSync(chave,m.entidade(),OperacaoSync.CRIAR,id,1L,m.dados()),new MutacaoSync(chave,m.entidade(),m.operacao(),UUID.randomUUID(),1L,m.dados()),new MutacaoSync(chave,m.entidade(),m.operacao(),id,2L,m.dados()),mutacao("{\"itens\":[2,1]}"),mutacao("{\"itens\":[1,3]}")))assertThat(bytes(outro)).isNotEqualTo(original);
	}
	@Test void migracao025InterpretaSemExecutarSql()throws Exception{
		try(var recursos=new liquibase.resource.ClassLoaderResourceAccessor()){
			var log=new liquibase.parser.core.yaml.YamlChangeLogParser().parse("db/changelog/changes/025-sync-replay-seguro.yaml",new liquibase.changelog.ChangeLogParameters(),recursos);assertThat(log.getChangeSets()).hasSize(1);assertThat(log.getChangeSets().getFirst().getId()).isEqualTo("025-sync-replay-seguro");
		}
	}
}
