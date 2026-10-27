package com.br.dietamais.rotina.infra;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdesaoStore {
	public record Base(UUID vinculoId,Integer refeicoesPorDia,Instant primeiraPublicacao) {}
	public record Marca(UUID vinculoId,String status) {}
	private final JdbcTemplate jdbc;
	public AdesaoStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	/** Refeições da dieta publicada hoje e instante da primeira publicação de cada vínculo; vínculos sem snapshot ficam fora. */
	public List<Base> bases(Collection<UUID> vinculos){
		return jdbc.query("""
			select v.id, (select jsonb_array_length(s.conteudo->'refeicoes') from dieta_snapshot s join dieta d on d.id=s.dieta_id
				where d.vinculo_id=v.id and d.status='PUBLICADA' order by s.publicado_em desc limit 1) n,
				(select min(s.publicado_em) from dieta_snapshot s where s.vinculo_id=v.id) primeira
			from unnest(?::uuid[]) as v(id)
			""",(rs,i)->new Base(rs.getObject(1,UUID.class),rs.getObject(2,Integer.class),rs.getTimestamp(3)==null?null:rs.getTimestamp(3).toInstant()),
			(Object)vinculos.toArray(UUID[]::new)).stream().filter(b->b.primeiraPublicacao()!=null).toList();
	}
	/** Último status de cada refeição por dia, entre as datas (inclusive), por vínculo. */
	public List<Marca> marcas(Collection<UUID> vinculos,LocalDate de,LocalDate ate){
		return jdbc.query("""
			select distinct on (vinculo_id,refeicao_id,data) vinculo_id,status from checkin
			where vinculo_id=any(?::uuid[]) and data between ? and ? order by vinculo_id,refeicao_id,data,registrado_em desc,criado_em desc
			""",(rs,i)->new Marca(rs.getObject(1,UUID.class),rs.getString(2)),(Object)vinculos.toArray(UUID[]::new),Date.valueOf(de),Date.valueOf(ate));
	}
	public Map<UUID,List<Marca>> agrupar(List<Marca> marcas){
		var m=new HashMap<UUID,List<Marca>>();marcas.forEach(x->m.computeIfAbsent(x.vinculoId(),k->new java.util.ArrayList<>()).add(x));return m;
	}
}
