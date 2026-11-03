package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarVinculosAtuais implements ConsultarVinculos {
	private final JdbcTemplate jdbc;
	private final UsuarioAtual usuario;
	private final ConsultaNutricionistas nutricionistas;
	private final AuditoriaClinica auditoria;
	public ConsultarVinculosAtuais(JdbcTemplate jdbc,UsuarioAtual usuario,ConsultaNutricionistas nutricionistas,AuditoriaClinica auditoria) {
		this.jdbc=jdbc;this.usuario=usuario;this.nutricionistas=nutricionistas;this.auditoria=auditoria;
	}
	@Override @Transactional(readOnly=true)
	public List<VinculoResposta> atuais() {
		var contexto=usuario.obter();var pid=contexto.exigirPaciente();
		var vinculos=jdbc.query("select v.* from vinculo v join paciente p on p.id=v.paciente_id where p.id=? and p.usuario_id=? and v.excluido_em is null order by v.id",
				(rs,n)->{
					var consentimento=rs.getTimestamp("consentimento_em");var encerrado=rs.getTimestamp("encerrado_em");
					return new Linha(rs.getObject("id",UUID.class),rs.getObject("nutricionista_id",UUID.class),StatusVinculo.valueOf(rs.getString("status")),
							consentimento==null?null:consentimento.toInstant(),encerrado==null?null:encerrado.toInstant(),rs.getLong("versao"),rs.getTimestamp("atualizado_em").toInstant());
				},pid,contexto.usuarioId());
		var identidades=nutricionistas.porIds(vinculos.stream().map(Linha::nutri).collect(Collectors.toSet()));
		return vinculos.stream().map(vinculo->{
			var nutri=Objects.requireNonNull(identidades.get(vinculo.nutri()),"Identidade da nutri indisponível");
			auditoria.leitura(new ReferenciaClinica("vinculo",vinculo.id(),nutri.id(),pid,nutri.organizacaoId()));
			return new VinculoResposta(vinculo.id(),pid,new NutricionistaResumoResposta(nutri.id(),nutri.nome(),nutri.crn()),vinculo.status(),vinculo.consentimento(),vinculo.encerrado(),vinculo.versao(),vinculo.atualizado());
		}).toList();
	}
	private record Linha(UUID id,UUID nutri,StatusVinculo status,Instant consentimento,Instant encerrado,long versao,Instant atualizado) { }
}
