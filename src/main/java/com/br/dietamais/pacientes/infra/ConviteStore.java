package com.br.dietamais.pacientes.infra;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConviteStore {
	private final JdbcTemplate jdbc;
	public ConviteStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	public Linha consultar(String hash,boolean bloquear) {
		return jdbc.query("select c.*,v.nutricionista_id,v.paciente_id,v.status from convite c join vinculo v on v.id=c.vinculo_id where c.hash_codigo=? and v.excluido_em is null"+(bloquear?" for update of c,v":""),
			(rs,n)->new Linha(rs.getObject("id",UUID.class),rs.getObject("vinculo_id",UUID.class),rs.getObject("nutricionista_id",UUID.class),rs.getObject("paciente_id",UUID.class),
				rs.getString("destinatario_email"),rs.getString("status"),rs.getTimestamp("expira_em").toInstant(),rs.getTimestamp("usado_em")!=null,rs.getTimestamp("invalidado_em")!=null),hash).stream().findFirst().orElse(null);
	}
	public void invalidar(UUID vinculo,Instant agora){jdbc.update("update convite set invalidado_em=? where vinculo_id=? and usado_em is null and invalidado_em is null",Timestamp.from(agora),vinculo);}
	public boolean inserir(UUID id,UUID vinculo,String hash,String email,Instant agora,Instant fim) {
		return jdbc.update("insert into convite(id,vinculo_id,hash_codigo,destinatario_email,criado_em,expira_em) values(?,?,?,?,?,?) on conflict(hash_codigo) do nothing",id,vinculo,hash,email,Timestamp.from(agora),Timestamp.from(fim))==1;
	}
	public UUID identificarPaciente(UUID original,UUID usuario) {
		var identidades=jdbc.query("select id from paciente where usuario_id=?",(rs,n)->rs.getObject(1,UUID.class),usuario);
		if(!identidades.isEmpty())return identidades.getFirst();
		int ligados=jdbc.update("update paciente set usuario_id=? where id=? and (usuario_id is null or usuario_id=?)",usuario,original,usuario);
		if(ligados!=1)throw new ExcecaoNegocio(CodigoErro.CONVITE_INVALIDO,"RN-VIN-02");
		return original;
	}
	public boolean duplicado(UUID vinculo,UUID paciente,UUID nutri) {
		return jdbc.queryForObject("select exists(select 1 from vinculo where nutricionista_id=? and paciente_id=? and id<>? and status in ('CONVIDADO','ATIVO') and excluido_em is null)",Boolean.class,nutri,paciente,vinculo);
	}
	public void aceitar(Linha convite,UUID paciente,UUID usuario,String versao,String hashTermo,String ip,Instant agora) {
		jdbc.update("update vinculo set paciente_origem_id=coalesce(paciente_origem_id,paciente_id),paciente_id=?,status='ATIVO',consentimento_em=?,versao=versao+1,atualizado_em=? where id=?",paciente,Timestamp.from(agora),Timestamp.from(agora),convite.vinculo());
		jdbc.update("update ficha_paciente set paciente_id=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where vinculo_id=?",paciente,Timestamp.from(agora),convite.vinculo());
		jdbc.update("insert into consentimento(vinculo_id,usuario_id,versao_termo,hash_termo,origem_ip,aceito_em) values(?,?,?,?,cast(? as inet),?)",convite.vinculo(),usuario,versao,hashTermo,ip,Timestamp.from(agora));
		jdbc.update("update convite set usado_em=? where id=?",Timestamp.from(agora),convite.id());
		invalidar(convite.vinculo(),agora);
	}
	public VinculoDados vinculo(UUID id) {
		return jdbc.queryForObject("select * from vinculo where id=?",(rs,n)->new VinculoDados(rs.getObject("id",UUID.class),rs.getObject("nutricionista_id",UUID.class),rs.getObject("paciente_id",UUID.class),
			StatusVinculo.valueOf(rs.getString("status")),instante(rs,"consentimento_em"),instante(rs,"encerrado_em"),rs.getLong("versao"),rs.getTimestamp("atualizado_em").toInstant()),id);
	}
	public List<VinculoDados> paraEncerrar(UUID paciente,UUID nutri,UUID vinculo) {
		var parametros=new ArrayList<Object>();parametros.add(paciente);
		String filtro="paciente_id=? and excluido_em is null";
		if(nutri!=null){filtro+=" and nutricionista_id=?";parametros.add(nutri);}
		if(vinculo!=null){filtro+=" and id=?";parametros.add(vinculo);}
		var ids=jdbc.query("select id from vinculo where "+filtro+" order by case status when 'ENCERRADO' then 1 else 0 end,criado_em desc,id desc",(rs,n)->rs.getObject(1,UUID.class),parametros.toArray());
		return ids.stream().map(this::vinculo).toList();
	}
	public void encerrar(UUID vinculo,Instant agora) {
		jdbc.update("update vinculo set status='ENCERRADO',encerrado_em=?,versao=versao+1,atualizado_em=? where id=?",Timestamp.from(agora),Timestamp.from(agora),vinculo);
		jdbc.update("update ficha_paciente set versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where vinculo_id=?",Timestamp.from(agora),vinculo);
		invalidar(vinculo,agora);
	}
	private Instant instante(ResultSet rs,String campo) throws SQLException {var valor=rs.getTimestamp(campo);return valor==null?null:valor.toInstant();}
	public record Linha(UUID id,UUID vinculo,UUID nutri,UUID paciente,String email,String status,Instant fim,boolean usado,boolean invalidado) {
		@Override public String toString(){return "Convite[dados protegidos]";}
	}
	public record VinculoDados(UUID id,UUID nutri,UUID paciente,StatusVinculo status,Instant consentimento,Instant encerrado,long versao,Instant atualizado) { }
}
