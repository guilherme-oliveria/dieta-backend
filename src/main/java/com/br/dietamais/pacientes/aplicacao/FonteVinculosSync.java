package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FonteVinculosSync implements FonteSync<VinculoResposta> {
	private final DiarioSync diario;private final JdbcTemplate jdbc;private final ConsultaNutricionistas nutris;private final AuditoriaClinica auditoria;
	public FonteVinculosSync(DiarioSync diario,JdbcTemplate jdbc,ConsultaNutricionistas nutris,AuditoriaClinica auditoria){this.diario=diario;this.jdbc=jdbc;this.nutris=nutris;this.auditoria=auditoria;}
	@Override public ColecaoSync colecao(){return ColecaoSync.VINCULOS;}
	@Override public List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite){return diario.mudancasDesde(colecao(),seq,usuario,limite);}
	@Override public VinculoResposta carregar(ReferenciaSync referencia,ContextoUsuario usuario) {
		String filtro;UUID dono;
		if(usuario.perfil()==Perfil.NUTRICIONISTA){filtro="nutricionista_id=?";dono=usuario.exigirNutricionista();}
		else{filtro="paciente_id=? and status in ('ATIVO','ENCERRADO')";dono=usuario.exigirPaciente();}
		var linha=jdbc.query("select * from vinculo where id=? and excluido_em is null and "+filtro,(rs,n)->mapear(rs),referencia.id(),dono).stream().findFirst().orElse(null);
		var dados=linha==null?null:linha.dados();
		UUID nid=dados==null?referencia.nutricionistaId():dados.nutricionista().id(),pid=dados==null?referencia.pacienteId():dados.pacienteId();
		auditoria.leitura(new ReferenciaClinica("vinculo",referencia.id(),nid,pid,linha==null?usuario.organizacaoId():linha.organizacaoId()));return dados;
	}
	private Linha mapear(ResultSet rs) throws SQLException {
		UUID nid=rs.getObject("nutricionista_id",UUID.class);var nutri=Objects.requireNonNull(nutris.porIds(Set.of(nid)).get(nid));
		var consentimento=rs.getTimestamp("consentimento_em");var encerrado=rs.getTimestamp("encerrado_em");
		return new Linha(new VinculoResposta(rs.getObject("id",UUID.class),rs.getObject("paciente_id",UUID.class),new NutricionistaResumoResposta(nid,nutri.nome(),nutri.crn()),StatusVinculo.valueOf(rs.getString("status")),consentimento==null?null:consentimento.toInstant(),encerrado==null?null:encerrado.toInstant(),rs.getLong("versao"),rs.getTimestamp("atualizado_em").toInstant()),nutri.organizacaoId());
	}
	private record Linha(VinculoResposta dados,UUID organizacaoId){}
}
