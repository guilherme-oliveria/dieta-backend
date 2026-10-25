package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.dominio.BloqueioLogin;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class SessaoStore {
	private final JdbcTemplate jdbc;
	private final JsonMapper mapper;
	public SessaoStore(JdbcTemplate jdbc, JsonMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
	public BloqueioLogin bloquearTentativas(String hash, Instant agora) {
		jdbc.update("insert into bloqueio_login(identificador_hash,falhas,atualizado_em) values(?,'[]'::jsonb,?) on conflict do nothing",hash,ts(agora));
		return jdbc.queryForObject("select falhas,bloqueado_ate from bloqueio_login where identificador_hash=? for update",(rs,n) -> {
			var falhas=Arrays.stream(mapper.readValue(rs.getString("falhas"),String[].class)).map(Instant::parse).toList();
			var ate=rs.getTimestamp("bloqueado_ate");
			return new BloqueioLogin(falhas,ate==null?null:ate.toInstant());
		},hash);
	}
	public void salvarTentativas(String hash, BloqueioLogin estado, Instant agora) {
		jdbc.update("update bloqueio_login set falhas=cast(? as jsonb),bloqueado_ate=?,atualizado_em=? where identificador_hash=?",
				mapper.writeValueAsString(estado.falhas().stream().map(Instant::toString).toList()),ts(estado.bloqueadoAte()),ts(agora),hash);
	}
	public UUID bloquearContaPorEmail(String email) {
		return jdbc.query("select id from usuario where lower(btrim(email))=? for update",(rs,n)->rs.getObject(1,UUID.class),email).stream().findFirst().orElse(null);
	}
	public boolean bloquearConta(UUID id) {
		return !jdbc.query("select id from usuario where id=? for update",(rs,n)->rs.getObject(1,UUID.class),id).isEmpty();
	}
	public UUID organizacaoNutri(UUID nutricionista,UUID usuario) {
		return jdbc.query("select organizacao_id from nutricionista where id=? and usuario_id=?",(rs,n)->rs.getObject(1,UUID.class),nutricionista,usuario)
				.stream().findFirst().orElse(null);
	}
	public Conta consultarConta(UUID id) {
		return jdbc.query("""
			select u.*,n.id as nid,p.id as pid from usuario u
			left join nutricionista n on n.usuario_id=u.id left join paciente p on p.usuario_id=u.id where u.id=?
			""",(rs,n)->new Conta(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("email"),
				rs.getString("senha_hash"),rs.getBoolean("ativo"),rs.getTimestamp("email_confirmado_em")!=null,
				Perfil.valueOf(rs.getString("perfil")),rs.getObject("nid",UUID.class),rs.getObject("pid",UUID.class),rs.getString("fuso")),id).stream().findFirst().orElse(null);
	}
	public Refresh consultarRefresh(String hash) {
		return jdbc.query("select * from refresh_token where hash_token=?",(rs,n)->new Refresh(rs.getObject("id",UUID.class),
				rs.getObject("usuario_id",UUID.class),rs.getObject("familia_id",UUID.class),rs.getObject("dispositivo_id",UUID.class),
				Plataforma.valueOf(rs.getString("plataforma")),rs.getString("nome_aparelho"),Perfil.valueOf(rs.getString("perfil")),
				rs.getTimestamp("expira_em").toInstant(),rs.getTimestamp("consumido_em")!=null,rs.getTimestamp("revogado_em")!=null),hash).stream().findFirst().orElse(null);
	}
	public void guardar(UUID id, UUID usuario, UUID familia, DispositivoRequisicao dispositivo, Perfil perfil, String hash, Instant agora, Instant fim) {
		jdbc.update("""
			insert into refresh_token(id,usuario_id,familia_id,dispositivo_id,plataforma,nome_aparelho,perfil,hash_token,criado_em,expira_em)
			values(?,?,?,?,?,?,?,?,?,?)
			""",id,usuario,familia,dispositivo.id(),dispositivo.plataforma().name(),dispositivo.nomeAparelho(),perfil.name(),hash,ts(agora),ts(fim));
	}
	public void consumir(UUID id, Instant agora) { jdbc.update("update refresh_token set consumido_em=? where id=?",ts(agora),id); }
	public void revogarFamilia(UUID familia, Instant agora) { jdbc.update("update refresh_token set revogado_em=? where familia_id=? and revogado_em is null",ts(agora),familia); }
	public void revogarDispositivo(UUID usuario, UUID dispositivo, Instant agora) {
		jdbc.update("update refresh_token set revogado_em=? where usuario_id=? and dispositivo_id=? and revogado_em is null",ts(agora),usuario,dispositivo);
	}
	public void limparExpirados(Instant corte, Instant agora) {
		jdbc.update("delete from refresh_token where expira_em<=?",ts(agora));
		jdbc.update("delete from bloqueio_login where atualizado_em<? and (bloqueado_ate is null or bloqueado_ate<=?)",ts(corte),ts(agora));
	}
	private static Timestamp ts(Instant instante) { return instante==null?null:Timestamp.from(instante); }
	public record Conta(UUID id,String nome,String email,String senhaHash,boolean ativo,boolean emailConfirmado,
			Perfil legado,UUID nid,UUID pid,String fuso) {
		public List<Perfil> perfis() {
			if (legado==Perfil.ADMIN) return List.of(Perfil.ADMIN);
			var perfis=new ArrayList<Perfil>();
			if(nid!=null)perfis.add(Perfil.NUTRICIONISTA);if(pid!=null)perfis.add(Perfil.PACIENTE);
			return List.copyOf(perfis);
		}
		public UsuarioResposta resposta(Perfil perfil) { return new UsuarioResposta(id,nome,email,emailConfirmado,perfis(),perfil,nid,pid,fuso); }
		@Override public String toString() { return "Conta[credenciais protegidas]"; }
	}
	public record Refresh(UUID id,UUID usuario,UUID familia,UUID dispositivo,Plataforma plataforma,String nomeAparelho,
			Perfil perfil,Instant expiraEm,boolean consumido,boolean revogado) { }
}
