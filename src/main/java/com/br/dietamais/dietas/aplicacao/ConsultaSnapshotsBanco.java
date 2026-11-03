package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.infra.DietaStore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ConsultaSnapshotsBanco implements ConsultaSnapshotsPaciente {
	private final DietaStore dietas;private final JsonMapper mapper;
	public ConsultaSnapshotsBanco(DietaStore dietas,JsonMapper mapper){this.dietas=dietas;this.mapper=mapper;}
	@Override public Optional<SnapshotParaRotina> obter(UUID dietaId,UUID paciente){
		return dietas.snapshotsDoPaciente(paciente).stream().filter(r->r.dietaId().equals(dietaId)).findFirst().map(r->new SnapshotParaRotina(
			ConsultarDietasPaciente.doPaciente(mapper.readValue(r.conteudo(),DietaSnapshotResposta.class),paciente),r.statusVinculo().equals("ATIVO"),r.statusDieta().equals("PUBLICADA")));
	}
}
