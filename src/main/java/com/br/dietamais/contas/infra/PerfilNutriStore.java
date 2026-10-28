package com.br.dietamais.contas.infra;

import com.br.dietamais.contas.api.PerfilNutriResposta;
import com.br.dietamais.contas.dominio.DadosPerfilNutri;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PerfilNutriStore {
	private final JdbcTemplate jdbc;
	public PerfilNutriStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public PerfilNutriResposta consultar(UUID nutri,UUID usuario,UUID organizacao,boolean bloquear){
		return jdbc.query("""
			select n.id,coalesce(n.nome_exibicao,u.nome) as nome,n.crn,n.telefone,n.orientacao_padrao,n.logo_chave is not null as tem_logo,n.versao,n.atualizado_em
			from nutricionista n join usuario u on u.id=n.usuario_id
			where n.id=? and n.usuario_id=? and n.organizacao_id=?
			"""+(bloquear?" for update of n":""),(rs,n)->new PerfilNutriResposta(rs.getObject("id",UUID.class),rs.getString("nome"),
				rs.getString("crn"),rs.getString("telefone"),rs.getString("orientacao_padrao"),rs.getBoolean("tem_logo"),rs.getLong("versao"),rs.getTimestamp("atualizado_em").toInstant()),
			nutri,usuario,organizacao).stream().findFirst().orElse(null);
	}
	public void atualizar(UUID nutri,UUID usuario,DadosPerfilNutri dados,Instant agora){
		jdbc.update("""
			update nutricionista set nome_exibicao=?,crn_verificado=case when crn<>? then false else crn_verificado end,
			crn=?,telefone=?,orientacao_padrao=?,versao=versao+1,atualizado_em=? where id=? and usuario_id=?
			""",dados.nome(),dados.crn(),dados.crn(),dados.telefone(),dados.orientacaoPadrao(),Timestamp.from(agora),nutri,usuario);
	}
	public String logoChave(UUID nutri){
		return jdbc.query("select logo_chave from nutricionista where id=?",(rs,n)->java.util.Optional.ofNullable(rs.getString(1)),nutri).stream().findFirst().flatMap(o->o).orElse(null);
	}
	public void definirLogo(UUID nutri,String chave){jdbc.update("update nutricionista set logo_chave=? where id=?",chave,nutri);}
}
