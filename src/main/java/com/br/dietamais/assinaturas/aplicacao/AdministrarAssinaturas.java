package com.br.dietamais.assinaturas.aplicacao;

import com.br.dietamais.assinaturas.api.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.OcupacaoPlano;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Planos e assinaturas pelo admin da plataforma (BE-30). Toda alteração vai para o {@code log_admin}. */
@Service
public class AdministrarAssinaturas {
	private final JdbcTemplate jdbc;private final RegistroAdmin registro;private final OcupacaoPlano ocupacao;private final Clock relogio;
	public AdministrarAssinaturas(JdbcTemplate jdbc,RegistroAdmin registro,OcupacaoPlano ocupacao,Clock relogio){this.jdbc=jdbc;this.registro=registro;this.ocupacao=ocupacao;this.relogio=relogio;}

	private static final String COLUNAS_PLANO="id,codigo,nome,preco_mensal,limite_pacientes,duracao_dias,ativo,criado_em";
	private static PlanoAdminResposta plano(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
		return new PlanoAdminResposta(rs.getObject("id",UUID.class),rs.getString("codigo"),rs.getString("nome"),rs.getBigDecimal("preco_mensal"),rs.getInt("limite_pacientes"),
			(Integer)rs.getObject("duracao_dias"),rs.getBoolean("ativo"),rs.getTimestamp("criado_em").toInstant());
	}

	@Transactional(readOnly=true)
	public List<PlanoAdminResposta> planos(){return jdbc.query("select "+COLUNAS_PLANO+" from plano order by preco_mensal,codigo",AdministrarAssinaturas::plano);}

	@Transactional
	public PlanoAdminResposta criarPlano(CriarPlanoRequisicao r){
		if(r.precoMensal().signum()>0&&r.duracaoDias()!=null)throw invalido("duracaoDias","Duração só vale para plano de teste (preço zero).");
		if(r.codigo().equals("TESTE")&&r.duracaoDias()==null)throw invalido("duracaoDias","O plano TESTE precisa de duração.");
		if(jdbc.queryForObject("select exists(select 1 from plano where codigo=?)",Boolean.class,r.codigo()))throw new ExcecaoNegocio(CodigoErro.VINCULO_JA_EXISTE,null,"Já existe um plano com este código.",List.of(),null);
		var id=jdbc.queryForObject("insert into plano(codigo,nome,preco_mensal,limite_pacientes,duracao_dias) values(?,?,?,?,?) returning id",UUID.class,r.codigo(),r.nome().strip(),r.precoMensal(),r.limitePacientes(),r.duracaoDias());
		var criado=umPlano(id);registro.registrar("PLANO_CRIADO","plano",id,null,criado,r.motivo());return criado;
	}

	@Transactional
	public PlanoAdminResposta atualizarPlano(UUID id,AtualizarPlanoRequisicao r){
		var antes=jdbc.query("select "+COLUNAS_PLANO+" from plano where id=? for update",AdministrarAssinaturas::plano,id).stream().findFirst()
			.orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		if(antes.codigo().equals("TESTE")){
			if(r.duracaoDias()==null)throw invalido("duracaoDias","O plano TESTE precisa de duração.");
			if(!r.ativo())throw invalido("ativo","O plano TESTE não pode ser desativado: é o plano de quem se cadastra.");
		}else if(r.precoMensal().signum()>0&&r.duracaoDias()!=null)throw invalido("duracaoDias","Duração só vale para plano de teste (preço zero).");
		jdbc.update("update plano set nome=?,preco_mensal=?,limite_pacientes=?,duracao_dias=?,ativo=? where id=?",r.nome().strip(),r.precoMensal(),r.limitePacientes(),r.duracaoDias(),r.ativo(),id);
		var depois=umPlano(id);registro.registrar("PLANO_ATUALIZADO","plano",id,antes,depois,r.motivo());return depois;
	}

	private PlanoAdminResposta umPlano(UUID id){return jdbc.queryForObject("select "+COLUNAS_PLANO+" from plano where id=?",AdministrarAssinaturas::plano,id);}

	private static final String BASE_ASSINATURA="""
		select a.id,a.organizacao_id,o.nome as organizacao_nome,p.codigo as plano_codigo,a.status,a.inicio_em,a.fim_periodo_em,a.gateway_assinatura_id is not null as gateway
		from assinatura a join organizacao o on o.id=a.organizacao_id join plano p on p.id=a.plano_id
		""";
	private AssinaturaAdminResposta mapearAssinatura(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
		var org=rs.getObject("organizacao_id",UUID.class);var fim=rs.getTimestamp("fim_periodo_em");
		return new AssinaturaAdminResposta(rs.getObject("id",UUID.class),org,rs.getString("organizacao_nome"),rs.getString("plano_codigo"),StatusAssinatura.valueOf(rs.getString("status")),
			rs.getTimestamp("inicio_em").toInstant(),fim==null?null:fim.toInstant(),ocupacao.daOrganizacao(org),rs.getBoolean("gateway"));
	}

	@Transactional(readOnly=true)
	public PaginaAssinaturasAdminResposta assinaturas(StatusAssinatura status,String busca,int pagina,int tamanho){
		if(pagina<0||tamanho<1||tamanho>100||busca!=null&&busca.length()>150)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		var args=new ArrayList<Object>();var where=new StringBuilder(" where true");
		if(status!=null){where.append(" and a.status=?");args.add(status.name());}
		if(busca!=null&&!busca.isBlank()){where.append(" and strpos(lower(o.nome),lower(?))>0");args.add(busca.strip());}
		long total=jdbc.queryForObject("select count(*) from assinatura a join organizacao o on o.id=a.organizacao_id"+where,Long.class,args.toArray());
		args.add(tamanho);args.add((long)pagina*tamanho);
		var itens=jdbc.query(BASE_ASSINATURA+where+" order by a.inicio_em desc,a.id limit ? offset ?",this::mapearAssinatura,args.toArray());
		return new PaginaAssinaturasAdminResposta(itens,pagina,tamanho,total);
	}

	@Transactional(readOnly=true)
	public AssinaturaAdminResposta assinatura(UUID id){
		return jdbc.query(BASE_ASSINATURA+" where a.id=?",this::mapearAssinatura,id).stream().findFirst().orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
	}

	@Transactional
	public AssinaturaAdminResposta ajustar(UUID id,AjustarAssinaturaRequisicao r){
		if(r.planoCodigo()==null&&r.status()==null&&r.fimPeriodoEm()==null)throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO,null,"Informe ao menos um campo para ajustar.",List.of(),null);
		jdbc.query("select id from assinatura where id=? for update",(rs,n)->0,id).stream().findFirst().orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		var antes=assinatura(id);
		if(r.planoCodigo()!=null){
			var planoId=jdbc.query("select id from plano where codigo=?",(rs,n)->rs.getObject(1,UUID.class),r.planoCodigo()).stream().findFirst().orElseThrow(()->invalido("planoCodigo","Plano inexistente."));
			jdbc.update("update assinatura set plano_id=? where id=?",planoId,id);
		}
		if(r.status()!=null)jdbc.update("update assinatura set status=? where id=?",r.status().name(),id);
		if(r.fimPeriodoEm()!=null)jdbc.update("update assinatura set fim_periodo_em=? where id=?",Timestamp.from(r.fimPeriodoEm()),id);
		var depois=assinatura(id);
		registro.registrar("ASSINATURA_AJUSTADA","assinatura",id,antes,depois,r.motivo());
		return depois;
	}

	private static ExcecaoNegocio invalido(String campo,String mensagem){
		return new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null,mensagem,List.of(new ErroCampoResposta(campo,"FORMATO_INVALIDO",mensagem)),null);
	}
}
