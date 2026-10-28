package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.api.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Conferência de CRN pelo admin (BE-30). Só dados profissionais; a verificação fica no log do admin. */
@Service
public class AdministrarNutricionistas {
	private static final String BASE="select n.id,coalesce(n.nome_exibicao,u.nome) as nome,u.email,n.crn,n.crn_verificado,n.organizacao_id from nutricionista n join usuario u on u.id=n.usuario_id";
	private final JdbcTemplate jdbc;private final RegistroAdmin registro;
	public AdministrarNutricionistas(JdbcTemplate jdbc,RegistroAdmin registro){this.jdbc=jdbc;this.registro=registro;}
	private static NutriAdminResposta nutri(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
		return new NutriAdminResposta(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("email"),rs.getString("crn"),rs.getBoolean("crn_verificado"),rs.getObject("organizacao_id",UUID.class));
	}
	@Transactional(readOnly=true)
	public PaginaNutrisAdminResposta listar(String busca,Boolean verificado,int pagina,int tamanho){
		if(pagina<0||tamanho<1||tamanho>100||busca!=null&&busca.length()>150)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		var args=new ArrayList<Object>();var where=new StringBuilder(" where true");
		if(verificado!=null){where.append(" and n.crn_verificado=?");args.add(verificado);}
		if(busca!=null&&!busca.isBlank()){where.append(" and (strpos(lower(coalesce(n.nome_exibicao,u.nome)),lower(?))>0 or strpos(lower(u.email),lower(?))>0 or strpos(lower(n.crn),lower(?))>0)");args.add(busca.strip());args.add(busca.strip());args.add(busca.strip());}
		long total=jdbc.queryForObject("select count(*) from nutricionista n join usuario u on u.id=n.usuario_id"+where,Long.class,args.toArray());
		args.add(tamanho);args.add((long)pagina*tamanho);
		var itens=jdbc.query(BASE+where+" order by lower(coalesce(n.nome_exibicao,u.nome)),n.id limit ? offset ?",AdministrarNutricionistas::nutri,args.toArray());
		return new PaginaNutrisAdminResposta(itens,pagina,tamanho,total);
	}
	@Transactional
	public NutriAdminResposta verificarCrn(UUID id,VerificarCrnRequisicao r){
		var antes=jdbc.query(BASE+" where n.id=? for update of n",AdministrarNutricionistas::nutri,id).stream().findFirst().orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		jdbc.update("update nutricionista set crn_verificado=? where id=?",r.verificado(),id);
		var depois=new NutriAdminResposta(antes.id(),antes.nome(),antes.email(),antes.crn(),r.verificado(),antes.organizacaoId());
		registro.registrar(r.verificado()?"CRN_VERIFICADO":"CRN_VERIFICACAO_REMOVIDA","nutricionista",id,antes,depois,r.motivo());
		return depois;
	}
}
