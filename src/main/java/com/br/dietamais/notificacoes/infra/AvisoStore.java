package com.br.dietamais.notificacoes.infra;

import com.br.dietamais.notificacoes.api.AvisoResposta;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AvisoStore {
	private static final String SELECT="select id,tipo,titulo,corpo,rota,criado_em,lido_em,versao from aviso ";
	private static final String DONO="usuario_id=? and perfil=? and excluido_em is null";
	private final JdbcTemplate jdbc;
	public AvisoStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	private static final RowMapper<AvisoResposta> MAPA=(rs,i)->new AvisoResposta(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),
		rs.getTimestamp(6).toInstant(),rs.getTimestamp(7)==null?null:rs.getTimestamp(7).toInstant(),rs.getLong(8));

	public List<AvisoResposta> listar(UUID usuario,String perfil,boolean soNaoLidos,int pagina,int tamanho){
		return jdbc.query(SELECT+"where "+DONO+(soNaoLidos?" and lido_em is null":"")+" order by criado_em desc,id desc limit ? offset ?",MAPA,usuario,perfil,tamanho,(long)pagina*tamanho);
	}
	public long contar(UUID usuario,String perfil,boolean soNaoLidos){
		return jdbc.queryForObject("select count(*) from aviso where "+DONO+(soNaoLidos?" and lido_em is null":""),Long.class,usuario,perfil);
	}
	public AvisoResposta doUsuario(UUID id,UUID usuario,String perfil){
		return jdbc.query(SELECT+"where id=? and "+DONO,MAPA,id,usuario,perfil).stream().findFirst().orElse(null);
	}
	/** Só escreve se ainda não foi lido: assim o sync não registra mudança repetida. */
	public void marcarLido(UUID id,UUID usuario,String perfil,Instant agora){
		jdbc.update("update aviso set lido_em=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and "+DONO+" and lido_em is null",Timestamp.from(agora),Timestamp.from(agora),id,usuario,perfil);
	}
	public int marcarTodosLidos(UUID usuario,String perfil,Instant agora){
		return jdbc.update("update aviso set lido_em=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where "+DONO+" and lido_em is null",Timestamp.from(agora),Timestamp.from(agora),usuario,perfil);
	}
	/** Histórico de 90 dias: o resto vira exclusão lógica (e some do aparelho pelo sync). Só avisos cujo push já foi resolvido. */
	public int arquivarAnteriores(Instant limite,Instant agora){
		return jdbc.update("update aviso set excluido_em=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where criado_em<? and excluido_em is null and push_status<>'PENDENTE'",Timestamp.from(agora),Timestamp.from(agora),Timestamp.from(limite));
	}
}
