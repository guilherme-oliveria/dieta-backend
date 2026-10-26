package com.br.dietamais.pacientes.infra;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.pacientes.dominio.DadosFicha;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class FichaPacienteStore {
	private static final String PROJECAO="select f.*,v.status from ficha_paciente f join vinculo v on v.id=f.vinculo_id";
	private static final String VISIBILIDADE=" f.nutricionista_id=? and v.nutricionista_id=? and f.paciente_id=v.paciente_id and f.excluido_em is null";
	private final JdbcTemplate jdbc;
	public FichaPacienteStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public void bloquearEscrita(UUID nutri) {
		jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,nutri.toString());
	}
	public PacienteResposta consultar(UUID paciente,UUID nutri,boolean bloquear) {
		return jdbc.query(PROJECAO+" where"+VISIBILIDADE+" and f.paciente_id=? order by case v.status when 'ATIVO' then 0 when 'CONVIDADO' then 1 else 2 end,f.criado_em desc,f.id desc limit 1"+(bloquear?" for update of f,v":""),this::mapear,nutri,nutri,paciente)
			.stream().findFirst().orElse(null);
	}
	public boolean emailOcupado(String email,UUID nutri,UUID exceto) {
		if(email==null)return false;
		return jdbc.queryForObject("select exists(select 1 from ficha_paciente f join vinculo v on v.id=f.vinculo_id where"+VISIBILIDADE+
			" and lower(btrim(f.email))=? and v.status in ('CONVIDADO','ATIVO') and (?::uuid is null or f.paciente_id<>?))",Boolean.class,nutri,nutri,email,exceto,exceto);
	}
	public void criar(UUID paciente,UUID vinculo,UUID nutri,DadosFicha d,Instant agora) {
		int inseridos=jdbc.update("insert into paciente(id,nome,email,data_nascimento,sexo,criado_em) values(?,?,?,?,?,?) on conflict(id) do nothing",
			paciente,d.nome(),d.email(),d.dataNascimento(),sexo(d),Timestamp.from(agora));
		if(inseridos==0)throw new ExcecaoNegocio(CodigoErro.VINCULO_JA_EXISTE,"RN-VIN-07");
		jdbc.update("insert into vinculo(id,nutricionista_id,paciente_id,status,criado_em,atualizado_em) values(?,?,?,'CONVIDADO',?,?)",vinculo,nutri,paciente,Timestamp.from(agora),Timestamp.from(agora));
		jdbc.update("""
			insert into ficha_paciente(id,vinculo_id,nutricionista_id,paciente_id,nome,email,data_nascimento,sexo,telefone,estatura_referencia,objetivo,observacoes,criado_em,atualizado_em)
			values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			""",vinculo,vinculo,nutri,paciente,d.nome(),d.email(),d.dataNascimento(),sexo(d),d.telefone(),d.estaturaReferencia(),d.objetivo(),d.observacoes(),Timestamp.from(agora),Timestamp.from(agora));
	}
	public void atualizar(UUID vinculo,UUID nutri,DadosFicha d,Instant agora) {
		jdbc.update("""
			update ficha_paciente set nome=?,email=?,data_nascimento=?,sexo=?,telefone=?,estatura_referencia=?,objetivo=?,observacoes=?,
			versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where vinculo_id=? and nutricionista_id=? and excluido_em is null
			""",d.nome(),d.email(),d.dataNascimento(),sexo(d),d.telefone(),d.estaturaReferencia(),d.objetivo(),d.observacoes(),Timestamp.from(agora),vinculo,nutri);
	}
	public PaginaPacientesResposta listar(UUID nutri,int pagina,int tamanho,String ordem,String busca,StatusVinculo status) {
		var argumentos=new ArrayList<Object>(List.of(nutri,nutri));
		String where=" where"+VISIBILIDADE;
		if(busca!=null&&!busca.isBlank()) {where+=" and strpos(lower(f.nome),lower(?))>0";argumentos.add(busca.strip());}
		if(status!=null){where+=" and v.status=?";argumentos.add(status.name());}
		long total=jdbc.queryForObject("select count(*) from ficha_paciente f join vinculo v on v.id=f.vinculo_id"+where,Long.class,argumentos.toArray());
		argumentos.add(tamanho);argumentos.add((long)pagina*tamanho);
		var itens=jdbc.query(PROJECAO+where+" order by "+ordem+",f.paciente_id,f.id limit ? offset ?",this::mapear,argumentos.toArray());
		return new PaginaPacientesResposta(itens,pagina,tamanho,total);
	}
	private PacienteResposta mapear(ResultSet rs,int linha) throws SQLException {
		var sexo=rs.getString("sexo");var nascimento=rs.getDate("data_nascimento");
		return new PacienteResposta(rs.getObject("paciente_id",UUID.class),rs.getObject("vinculo_id",UUID.class),rs.getString("nome"),rs.getString("email"),
			nascimento==null?null:nascimento.toLocalDate(),sexo==null?null:Sexo.valueOf(sexo),rs.getString("telefone"),rs.getBigDecimal("estatura_referencia"),
			rs.getString("objetivo"),rs.getString("observacoes"),StatusVinculo.valueOf(rs.getString("status")),rs.getLong("versao"),rs.getTimestamp("criado_em").toInstant(),rs.getTimestamp("atualizado_em").toInstant(),null);
	}
	private String sexo(DadosFicha dados){return dados.sexo()==null?null:dados.sexo().name();}
}
