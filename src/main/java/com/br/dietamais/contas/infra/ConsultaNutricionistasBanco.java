package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsultaNutricionistasBanco implements ConsultaNutricionistas {
	private final JdbcTemplate jdbc;
	private final UsuarioAtual usuario;
	public ConsultaNutricionistasBanco(JdbcTemplate jdbc,UsuarioAtual usuario){this.jdbc=jdbc;this.usuario=usuario;}
	@Override public Map<UUID,DadosNutri> porIds(Set<UUID> ids) {
		if(ids.isEmpty())return Map.of();
		String parametros=String.join(",",Collections.nCopies(ids.size(),"?"));
		var lista=jdbc.query("select n.id,u.nome,n.crn,n.organizacao_id from nutricionista n join usuario u on u.id=n.usuario_id where n.id in ("+parametros+")",
				(rs,n)->new DadosNutri(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("crn"),rs.getObject("organizacao_id",UUID.class)),ids.toArray());
		var dados=new HashMap<UUID,DadosNutri>();lista.forEach(item->dados.put(item.id(),item));return Map.copyOf(dados);
	}
	@Override public List<UUID> daOrganizacaoAtual() {
		var contexto=usuario.obter();contexto.exigirNutricionista();
		return jdbc.query("select id from nutricionista where organizacao_id=?",(rs,n)->rs.getObject(1,UUID.class),contexto.organizacaoId());
	}
}
