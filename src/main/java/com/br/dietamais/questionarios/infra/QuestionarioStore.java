package com.br.dietamais.questionarios.infra;

import com.br.dietamais.questionarios.api.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class QuestionarioStore {
	/** Estado mínimo para decidir uma gravação sob bloqueio de linha. */
	public record Estado(UUID id,UUID vinculoId,UUID nutricionistaId,UUID pacienteId,StatusQuestionario status,List<BlocoQuestionario> blocos,long versao) {}
	private static final TypeReference<List<BlocoQuestionario>> BLOCOS=new TypeReference<>(){};
	private final JdbcTemplate jdbc;private final JsonMapper mapper;
	public QuestionarioStore(JdbcTemplate jdbc,JsonMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}

	public void bloquear(UUID nutri){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"questionarios:"+nutri);}

	// ---- modelos ----
	public boolean temPadrao(UUID nutri){
		return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from modelo_questionario where nutricionista_id=? and padrao and excluido_em is null)",Boolean.class,nutri));
	}
	public boolean temAnamneseCompleta(UUID nutri){
		return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from modelo_questionario where nutricionista_id=? and origem='ANAMNESE_COMPLETA' and excluido_em is null)",Boolean.class,nutri));
	}
	public void identificarAnamneseCompleta(UUID modelo){jdbc.update("update modelo_questionario set origem='ANAMNESE_COMPLETA' where id=?",modelo);}
	public boolean inserirModelo(UUID id,UUID nutri,String nome,boolean padrao,List<BlocoQuestionario> blocos,Instant agora){
		int n=jdbc.update("insert into modelo_questionario(id,nutricionista_id,nome,padrao,criado_em,atualizado_em) values(?,?,?,?,?,?) on conflict(id) do nothing",id,nutri,nome,padrao,Timestamp.from(agora),Timestamp.from(agora));
		if(n==1)gravarPerguntas(id,blocos);
		return n==1;
	}
	public void substituirModelo(UUID id,UUID nutri,String nome,List<BlocoQuestionario> blocos,Instant agora){
		jdbc.update("update modelo_questionario set nome=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=? and nutricionista_id=? and excluido_em is null",nome,Timestamp.from(agora),id,nutri);
		jdbc.update("delete from pergunta where modelo_id=?",id);
		gravarPerguntas(id,blocos);
	}
	private void gravarPerguntas(UUID modelo,List<BlocoQuestionario> blocos){
		for(int b=0;b<blocos.size();b++){
			var bloco=blocos.get(b);
			for(int i=0;i<bloco.perguntas().size();i++){
				var p=bloco.perguntas().get(i);
				jdbc.update("insert into pergunta(modelo_id,id,bloco_ordem,bloco_titulo,ordem,texto,tipo,opcoes,obrigatoria,condicao,codigo) values(?,?,?,?,?,?,?,?::jsonb,?,?::jsonb,?)",
					modelo,p.id(),b,bloco.titulo(),i,p.texto(),p.tipo().name(),p.opcoes()==null?null:mapper.writeValueAsString(p.opcoes()),p.obrigatoria(),p.condicao()==null?null:mapper.writeValueAsString(p.condicao()),p.codigo());
			}
		}
	}
	/** Versão atual e dono, com bloqueio de linha, para o {@code If-Match} do PUT. */
	public Long versaoDoModelo(UUID id,UUID nutri,boolean bloquear){
		return jdbc.query("select versao from modelo_questionario where id=? and nutricionista_id=? and excluido_em is null"+(bloquear?" for update":""),(rs,n)->rs.getLong(1),id,nutri).stream().findFirst().orElse(null);
	}
	public ModeloQuestionarioResposta modelo(UUID id,UUID nutri){
		var l=modelos("where m.id=? and m.nutricionista_id=? and m.excluido_em is null",id,nutri);return l.isEmpty()?null:l.get(0);
	}
	public List<ModeloQuestionarioResposta> modelosDaNutri(UUID nutri){return modelos("where m.nutricionista_id=? and m.excluido_em is null",nutri);}
	private List<ModeloQuestionarioResposta> modelos(String filtro,Object... args){
		record Cab(UUID id,String nome,boolean padrao,long versao,Instant criado,Instant atualizado) {}
		var cabs=jdbc.query("select m.id,m.nome,m.padrao,m.versao,m.criado_em,m.atualizado_em from modelo_questionario m "+filtro+" order by m.padrao desc,m.criado_em,m.id",
			(rs,n)->new Cab(rs.getObject(1,UUID.class),rs.getString(2),rs.getBoolean(3),rs.getLong(4),rs.getTimestamp(5).toInstant(),rs.getTimestamp(6).toInstant()),args);
		if(cabs.isEmpty())return List.of();
		var ids=cabs.stream().map(Cab::id).toArray(UUID[]::new);
		var linhas=new LinkedHashMap<UUID,List<Linha>>();
		jdbc.query("select modelo_id,id,bloco_ordem,bloco_titulo,texto,tipo,opcoes,obrigatoria,condicao,codigo from pergunta where modelo_id=any(?::uuid[]) order by modelo_id,bloco_ordem,ordem",
			(org.springframework.jdbc.core.RowCallbackHandler)rs->linhas.computeIfAbsent(rs.getObject(1,UUID.class),k->new ArrayList<>()).add(linha(rs)),(Object)ids);
		return cabs.stream().map(c->new ModeloQuestionarioResposta(c.id(),c.nome(),c.padrao(),agrupar(linhas.getOrDefault(c.id(),List.of())),c.versao(),c.criado(),c.atualizado())).toList();
	}
	private record Linha(int bloco,String titulo,PerguntaQuestionario pergunta) {}
	private Linha linha(ResultSet rs) throws SQLException {
		var opcoes=rs.getString(7);var cond=rs.getString(9);
		return new Linha(rs.getInt(3),rs.getString(4),new PerguntaQuestionario(rs.getObject(2,UUID.class),rs.getString(5),TipoPergunta.valueOf(rs.getString(6)),
			opcoes==null?null:mapper.readValue(opcoes,new TypeReference<List<String>>(){}),rs.getBoolean(8),cond==null?null:mapper.readValue(cond,CondicaoPergunta.class),rs.getString(10)));
	}
	private List<BlocoQuestionario> agrupar(List<Linha> linhas){
		var porBloco=new LinkedHashMap<Integer,List<Linha>>();
		for(var l:linhas)porBloco.computeIfAbsent(l.bloco(),k->new ArrayList<>()).add(l);
		return porBloco.values().stream().map(ls->new BlocoQuestionario(ls.get(0).titulo(),ls.stream().map(Linha::pergunta).toList())).toList();
	}

	// ---- questionários ----
	public boolean inserirQuestionario(UUID id,UUID vinculo,UUID nutri,UUID paciente,UUID modelo,String nome,List<BlocoQuestionario> blocos,Instant agora){
		return jdbc.update("insert into questionario(id,vinculo_id,nutricionista_id,paciente_id,modelo_id,nome,blocos,enviado_em,criado_em,atualizado_em) values(?,?,?,?,?,?,?::jsonb,?,?,?) on conflict(id) do nothing",
			id,vinculo,nutri,paciente,modelo,nome,mapper.writeValueAsString(blocos),Timestamp.from(agora),Timestamp.from(agora),Timestamp.from(agora))==1;
	}
	public Estado estadoBloqueado(UUID id){
		return jdbc.query("select id,vinculo_id,nutricionista_id,paciente_id,status,blocos,versao from questionario where id=? and excluido_em is null for update",
			(rs,n)->new Estado(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getObject(4,UUID.class),StatusQuestionario.valueOf(rs.getString(5)),
				mapper.readValue(rs.getString(6),BLOCOS),rs.getLong(7)),id).stream().findFirst().orElse(null);
	}
	public boolean vinculoAtivo(UUID vinculo,UUID paciente){
		return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from vinculo where id=? and paciente_id=? and status='ATIVO' and excluido_em is null)",Boolean.class,vinculo,paciente));
	}
	public Map<UUID,JsonNode> respostasDe(UUID questionario){
		var m=new LinkedHashMap<UUID,JsonNode>();
		jdbc.query("select pergunta_id,valor from resposta where questionario_id=? order by pergunta_id",(org.springframework.jdbc.core.RowCallbackHandler)rs->m.put(rs.getObject(1,UUID.class),mapper.readTree(rs.getString(2))),questionario);
		return m;
	}
	/** Mescla (RN-ANA-03): {@code gravar} substitui, {@code apagar} remove; versão do questionário sobe para o sync entregar o novo estado. */
	public void mesclarRespostas(UUID questionario,Map<UUID,JsonNode> gravar,Set<UUID> apagar,Instant agora){
		for(var e:gravar.entrySet())
			jdbc.update("insert into resposta(questionario_id,pergunta_id,valor,atualizado_em) values(?,?,?::jsonb,?) on conflict(questionario_id,pergunta_id) do update set valor=excluded.valor,atualizado_em=excluded.atualizado_em",
				questionario,e.getKey(),mapper.writeValueAsString(e.getValue()),Timestamp.from(agora));
		for(var p:apagar)jdbc.update("delete from resposta where questionario_id=? and pergunta_id=?",questionario,p);
		jdbc.update("update questionario set versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=?",Timestamp.from(agora),questionario);
	}
	public void marcarRespondido(UUID questionario,Instant agora){
		jdbc.update("update questionario set status='RESPONDIDO',respondido_em=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where id=?",Timestamp.from(agora),Timestamp.from(agora),questionario);
	}
	public void gravarDerivados(UUID questionario,AnamneseDerivada derivados){
		jdbc.update("update questionario set derivados=?::jsonb where id=?",mapper.writeValueAsString(derivados),questionario);
	}
	public QuestionarioResposta daNutri(UUID id,UUID nutri){
		var l=questionarios("q.id=? and q.nutricionista_id=?",id,nutri);return l.isEmpty()?null:l.get(0);
	}
	public List<QuestionarioResposta> porVinculo(UUID vinculo,UUID nutri){return questionarios("q.vinculo_id=? and q.nutricionista_id=?",vinculo,nutri);}
	public List<QuestionarioResposta> doPaciente(UUID paciente){return questionarios("q.paciente_id=? and v.status in ('ATIVO','ENCERRADO')",paciente);}
	public QuestionarioResposta doPacienteUm(UUID id,UUID paciente){
		var l=questionarios("q.id=? and q.paciente_id=? and v.status in ('ATIVO','ENCERRADO')",id,paciente);return l.isEmpty()?null:l.get(0);
	}
	private List<QuestionarioResposta> questionarios(String filtro,Object... args){
		var base=jdbc.query("select q.id,q.paciente_id,q.vinculo_id,q.modelo_id,q.nome,q.blocos,q.status,q.enviado_em,q.respondido_em,q.versao,q.criado_em,q.atualizado_em,q.derivados from questionario q join vinculo v on v.id=q.vinculo_id "
			+"where "+filtro+" and q.excluido_em is null and v.excluido_em is null order by q.enviado_em desc,q.id desc",this::mapear,args);
		if(base.isEmpty())return base;
		var ids=base.stream().map(QuestionarioResposta::id).toArray(UUID[]::new);
		var respostas=new HashMap<UUID,Map<UUID,JsonNode>>();
		jdbc.query("select questionario_id,pergunta_id,valor from resposta where questionario_id=any(?::uuid[]) order by questionario_id,pergunta_id",(org.springframework.jdbc.core.RowCallbackHandler)rs->
			respostas.computeIfAbsent(rs.getObject(1,UUID.class),k->new LinkedHashMap<>()).put(rs.getObject(2,UUID.class),mapper.readTree(rs.getString(3))),(Object)ids);
		return base.stream().map(q->new QuestionarioResposta(q.id(),q.pacienteId(),q.vinculoId(),q.modeloId(),q.nome(),q.blocos(),respostas.getOrDefault(q.id(),Map.of()),q.status(),q.enviadoEm(),q.respondidoEm(),q.versao(),q.criadoEm(),q.atualizadoEm(),q.derivados())).collect(Collectors.toList());
	}
	private QuestionarioResposta mapear(ResultSet rs,int n) throws SQLException {
		var respondido=rs.getTimestamp(9);
		return new QuestionarioResposta(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getObject(4,UUID.class),rs.getString(5),mapper.readValue(rs.getString(6),BLOCOS),
			Map.of(),StatusQuestionario.valueOf(rs.getString(7)),rs.getTimestamp(8).toInstant(),respondido==null?null:respondido.toInstant(),rs.getLong(10),rs.getTimestamp(11).toInstant(),rs.getTimestamp(12).toInstant(),rs.getString(13)==null?null:mapper.readValue(rs.getString(13),AnamneseDerivada.class));
	}
}
