package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.infra.DietaStore;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ConsultaDietaParaPdfBanco implements ConsultaDietaParaPdf {
	private final DietaStore dietas;private final UsuarioAtual usuario;private final ConsultaSnapshotsPaciente snapshots;private final JsonMapper mapper;
	public ConsultaDietaParaPdfBanco(DietaStore dietas,UsuarioAtual usuario,ConsultaSnapshotsPaciente snapshots,JsonMapper mapper){this.dietas=dietas;this.usuario=usuario;this.snapshots=snapshots;this.mapper=mapper;}
	@Override @Transactional(readOnly=true)
	public DietaSnapshotResposta daNutri(UUID dietaId){
		var nutri=usuario.obter().exigirNutricionista();
		var conteudo=dietas.snapshotDaNutri(dietaId,nutri);
		if(conteudo==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return mapper.readValue(conteudo,DietaSnapshotResposta.class);
	}
	@Override @Transactional(readOnly=true)
	public DietaSnapshotResposta doPaciente(UUID dietaId){
		var paciente=usuario.obter().exigirPaciente();
		return snapshots.obter(dietaId,paciente).map(ConsultaSnapshotsPaciente.SnapshotParaRotina::snapshot).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
	}
}
