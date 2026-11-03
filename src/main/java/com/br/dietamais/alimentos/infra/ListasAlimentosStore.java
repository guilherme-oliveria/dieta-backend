package com.br.dietamais.alimentos.infra;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.dominio.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ListasAlimentosStore {
	private final JdbcTemplate jdbc;
	public ListasAlimentosStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public void bloquear(UUID nutri){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,nutri.toString());}
	public AlimentoResposta alimento(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query("select * from alimento_nutri where id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),this::mapear,id,nutri).stream().findFirst().orElse(null);
	}
	public boolean criar(UUID id,UUID nutri,DadosAlimento d,Instant agora){
		return jdbc.update("""
			insert into alimento_nutri(id,nutricionista_id,grupo_id,nome,medida_caseira,gramas_por_porcao,ativo,marcadores,criado_em,atualizado_em)
			values(?,?,?,?,?,?,true,?::text[],?,?) on conflict(id) do nothing
			""",id,nutri,d.grupoId(),d.nome(),d.medidaCaseira(),d.gramas(),marcadores(d.marcadores()==null?List.of():d.marcadores()),Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public void atualizar(UUID id,UUID nutri,DadosAlimento d,Instant agora){
		// marcadores ausentes: não mexe nos atuais (BE-51)
		if(d.marcadores()==null)jdbc.update("""
			update alimento_nutri set grupo_id=?,nome=?,medida_caseira=?,gramas_por_porcao=?,ativo=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=?
			where id=? and nutricionista_id=? and excluido_em is null
			""",d.grupoId(),d.nome(),d.medidaCaseira(),d.gramas(),d.ativo(),Timestamp.from(agora),id,nutri);
		else jdbc.update("""
			update alimento_nutri set grupo_id=?,nome=?,medida_caseira=?,gramas_por_porcao=?,ativo=?,marcadores=?::text[],versao=versao+1,seq=nextval('seq_sync'),atualizado_em=?
			where id=? and nutricionista_id=? and excluido_em is null
			""",d.grupoId(),d.nome(),d.medidaCaseira(),d.gramas(),d.ativo(),marcadores(d.marcadores()),Timestamp.from(agora),id,nutri);
	}
	public PaginaAlimentosResposta listar(UUID nutri,int pagina,int tamanho,UUID grupo,String busca,boolean ativo){return listar(nutri,pagina,tamanho,grupo,busca,ativo,List.of());}
	/** {@code sem}: só alimentos que não carregam nenhum destes marcadores (BE-51). */
	public PaginaAlimentosResposta listar(UUID nutri,int pagina,int tamanho,UUID grupo,String busca,boolean ativo,List<MarcadorAlimento> sem){
		String where=" where nutricionista_id=? and ativo=? and excluido_em is null";var argumentos=new ArrayList<Object>(List.of(nutri,ativo));
		if(sem!=null&&!sem.isEmpty()){where+=" and not (marcadores && ?::text[])";argumentos.add(marcadores(sem));}
		if(grupo!=null){where+=" and grupo_id=?";argumentos.add(grupo);}
		if(busca!=null&&!busca.isBlank()){where+=" and strpos(lower(nome),lower(?))>0";argumentos.add(busca.strip());}
		long total=jdbc.queryForObject("select count(*) from alimento_nutri"+where,Long.class,argumentos.toArray());
		argumentos.add(tamanho);argumentos.add((long)pagina*tamanho);
		var itens=jdbc.query("select * from alimento_nutri"+where+" order by lower(nome),id limit ? offset ?",this::mapear,argumentos.toArray());
		return new PaginaAlimentosResposta(itens,pagina,tamanho,total);
	}
	public boolean referenciasProprias(UUID nutri,List<UUID> ids,boolean ativo){
		if(ids.isEmpty())return true;
		long total=jdbc.queryForObject("select count(*) from alimento_nutri where nutricionista_id=? and id=any(?::uuid[]) and excluido_em is null"+(ativo?" and ativo":""),Long.class,nutri,array(ids));
		return total==ids.size();
	}
	public AjusteLista ajuste(UUID vinculo,UUID nutri,boolean bloquear){
		return jdbc.query("select * from ajuste_lista_paciente where vinculo_id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),
			(rs,n)->new AjusteLista(vinculo,ids(rs,"base_ids"),ids(rs,"adicionados"),ids(rs,"removidos"),rs.getLong("versao")),vinculo,nutri).stream().findFirst().orElse(null);
	}
	public List<AlimentoResposta> efetivos(AjusteLista ajuste,UUID nutri){
		var ids=new HashSet<>(ajuste.base());ids.addAll(ajuste.adicionados());ids.removeAll(ajuste.removidos());
		return jdbc.query("select * from alimento_nutri where nutricionista_id=? and ativo and excluido_em is null and id=any(?::uuid[]) order by lower(nome),id",this::mapear,nutri,array(ids));
	}
	public void atualizarAjuste(UUID vinculo,UUID nutri,DadosAjusteLista dados,Instant agora){
		jdbc.update("""
			update ajuste_lista_paciente set adicionados=?::uuid[],removidos=?::uuid[],versao=nextval('seq_versao_lista'),seq=nextval('seq_sync'),atualizado_em=?
			where vinculo_id=? and nutricionista_id=? and excluido_em is null
			""",array(dados.adicionados()),array(dados.removidos()),Timestamp.from(agora),vinculo,nutri);
	}
	private AlimentoResposta mapear(ResultSet rs,int n) throws SQLException {
		return new AlimentoResposta(rs.getObject("id",UUID.class),rs.getObject("catalogo_id",UUID.class),rs.getObject("grupo_id",UUID.class),rs.getString("nome"),rs.getString("medida_caseira"),rs.getBigDecimal("gramas_por_porcao"),rs.getBoolean("ativo"),rs.getLong("versao"),rs.getTimestamp("atualizado_em").toInstant(),marcadores(rs));
	}
	private List<UUID> ids(ResultSet rs,String coluna) throws SQLException {
		var array=rs.getArray(coluna);try{return List.copyOf(Arrays.asList((UUID[])array.getArray()));}finally{array.free();}
	}
	private static String marcadores(List<MarcadorAlimento> lista){return "{"+String.join(",",lista.stream().map(Enum::name).toList())+"}";}
	private static List<MarcadorAlimento> marcadores(ResultSet rs) throws SQLException {
		var a=rs.getArray("marcadores");if(a==null)return List.of();
		return Arrays.stream((String[])a.getArray()).map(MarcadorAlimento::valueOf).toList();
	}
	private String array(Collection<UUID> ids){return "{"+String.join(",",ids.stream().map(UUID::toString).sorted().toList())+"}";}
}
