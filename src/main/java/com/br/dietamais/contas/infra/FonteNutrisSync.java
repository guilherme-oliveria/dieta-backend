package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.NutricionistaResumoResposta;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FonteNutrisSync implements FonteSync<NutricionistaResumoResposta> {
	private final DiarioSync diario;private final JdbcTemplate jdbc;
	public FonteNutrisSync(DiarioSync diario,JdbcTemplate jdbc){this.diario=diario;this.jdbc=jdbc;}
	@Override public ColecaoSync colecao(){return ColecaoSync.NUTRIS;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	@Override public NutricionistaResumoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario) {
		String filtro;Object[] parametros;
		if(usuario.perfil()==Perfil.NUTRICIONISTA){filtro="n.id=? and n.usuario_id=?";parametros=new Object[]{referencia.id(),usuario.exigirNutricionista(),usuario.usuarioId()};}
		else{filtro="exists(select 1 from vinculo v where v.nutricionista_id=n.id and v.paciente_id=? and v.status in ('ATIVO','ENCERRADO') and v.excluido_em is null)";parametros=new Object[]{referencia.id(),usuario.exigirPaciente()};}
		return jdbc.query("select n.id,u.nome,n.crn from nutricionista n join usuario u on u.id=n.usuario_id where n.id=? and "+filtro,
			(rs,n)->new NutricionistaResumoResposta(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("crn")),parametros).stream().findFirst().orElse(null);
	}
}
