package com.br.dietamais.relatorios.aplicacao;

import com.br.dietamais.avaliacoes.api.AvaliacaoResposta;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.DietaSnapshotResposta;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.relatorios.infra.ExportacaoStore;
import com.br.dietamais.relatorios.infra.ExportacaoStore.Pedido;
import com.br.dietamais.sync.api.ColetaDadosPaciente;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Gera o ZIP com os dados do paciente (JSON com tudo que o app dele enxerga + PDFs das dietas e avaliações), guarda por 24 h e avisa por e-mail.
 * Nunca registra o conteúdo nem o e-mail em log.
 */
@Component
public class ProcessadorExportacao {
	public static final String PREFIXO="exportacao";
	static final Duration VALIDADE=Duration.ofHours(24);
	static final int MAX_TENTATIVAS=3;
	private final ExportacaoStore store;private final ContextoPaciente contextos;private final ColetaDadosPaciente coleta;private final ServicoPdf pdfs;
	private final Armazenamento armazenamento;private final EnfileirarEmail emails;private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;private final TransactionTemplate tx;
	private final SecureRandom aleatorio=new SecureRandom();
	public ProcessadorExportacao(ExportacaoStore store,ContextoPaciente contextos,ColetaDadosPaciente coleta,ServicoPdf pdfs,Armazenamento armazenamento,EnfileirarEmail emails,
			GeradorIds ids,Clock relogio,JsonMapper mapper,TransactionTemplate tx){
		this.store=store;this.contextos=contextos;this.coleta=coleta;this.pdfs=pdfs;this.armazenamento=armazenamento;this.emails=emails;this.ids=ids;this.relogio=relogio;this.mapper=mapper;this.tx=tx;
	}

	@Scheduled(fixedDelayString="${dieta.exportacao.intervalo:PT30S}",initialDelayString="PT20S")
	public void processar(){
		while(Boolean.TRUE.equals(tx.execute(s->processarUm()))) { }
		tx.executeWithoutResult(s->{
			for(var vencido:store.expirar(relogio.instant()))if(vencido.chave()!=null)armazenamento.apagar(vencido.chave());
		});
	}

	/** Pega um pedido pendente e o conclui; falha isolada não derruba os demais. */
	boolean processarUm(){
		var pedido=store.proximoPendente();
		if(pedido.isEmpty())return false;
		var p=pedido.get();
		try{concluir(p);}
		catch(RuntimeException erro){
			// sem detalhes no log: a mensagem pode conter dado de saúde
			Throwable causa=erro;while(causa.getCause()!=null)causa=causa.getCause();
			LoggerFactory.getLogger(ProcessadorExportacao.class).warn("Falha ao gerar exportação id={} tentativa={} causa={}",p.id(),p.tentativas()+1,causa.getClass().getName());
			store.falhou(p.id(),p.tentativas()+1>=MAX_TENTATIVAS);
		}
		return true;
	}

	private void concluir(Pedido p){
		var contexto=contextos.de(p.usuarioId());
		byte[] zip=ContextoSegundoPlano.como(contexto,()->montar(contexto));
		String chave=PREFIXO+"/"+ids.novo()+".zip";
		armazenamento.guardar(chave,zip,"application/zip");
		var agora=relogio.instant();var expira=agora.plus(VALIDADE);
		byte[] bytes=new byte[32];aleatorio.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		store.concluir(p.id(),chave,hash(token),agora,expira);
		emails.enfileirar(new EmailTransacional(TipoEmail.EXPORTACAO_PRONTA,contexto.usuario().email(),token,expira));
	}

	byte[] montar(ContextoUsuario contexto){
		var paginas=coleta.coletar(contexto);
		var dados=mapper.createObjectNode();
		var snapshots=new ArrayList<DietaSnapshotResposta>();var avaliacoes=new ArrayList<AvaliacaoResposta>();
		for(var pagina:paginas){
			ObjectNode arvore=(ObjectNode)mapper.valueToTree(pagina);
			for(var campo:arvore.properties()){
				ArrayNode destino=dados.has(campo.getKey())?(ArrayNode)dados.get(campo.getKey()):dados.putArray(campo.getKey());
				for(JsonNode mudanca:campo.getValue()){JsonNode d=mudanca.get("dados");if(d!=null&&!d.isNull())destino.add(d);}
			}
			if(pagina.snapshots()!=null)pagina.snapshots().forEach(m->{if(m.dados()!=null)snapshots.add(m.dados());});
			if(pagina.avaliacoes()!=null)pagina.avaliacoes().forEach(m->{if(m.dados()!=null)avaliacoes.add(m.dados());});
		}
		var raiz=mapper.createObjectNode();
		raiz.put("geradoEm",relogio.instant().toString());
		var titular=raiz.putObject("titular");
		titular.put("nome",contexto.usuario().nome());titular.put("email",contexto.usuario().email());titular.put("fuso",contexto.usuario().fuso());
		raiz.set("dados",dados);
		try(var saida=new ByteArrayOutputStream();var zip=new ZipOutputStream(saida,StandardCharsets.UTF_8)){
			entrada(zip,"dados.json",mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(raiz));
			entrada(zip,"LEIAME.txt",LEIAME.getBytes(StandardCharsets.UTF_8));
			for(var s:snapshots)entrada(zip,"dietas/"+ServicoPdf.slug(s.titulo())+"-"+curto(s.id())+".pdf",pdfs.dietaBytes(s));
			for(var a:avaliacoes)entrada(zip,"avaliacoes/avaliacao-"+a.dataHora().toString().substring(0,10)+"-"+curto(a.id())+".pdf",pdfs.avaliacaoBytes(a));
			zip.finish();
			return saida.toByteArray();
		}catch(java.io.IOException erro){throw new java.io.UncheckedIOException(erro);}
	}
	private static void entrada(ZipOutputStream zip,String nome,byte[] conteudo) throws java.io.IOException {
		zip.putNextEntry(new ZipEntry(nome));zip.write(conteudo);zip.closeEntry();
	}
	private static String curto(UUID id){return id.toString().substring(0,8);}
	public static String hash(String valor){
		try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));}
		catch(java.security.NoSuchAlgorithmException erro){throw new IllegalStateException("SHA-256 indisponível");}
	}
	static final String LEIAME="""
		Cópia dos seus dados no Dieta+ (LGPD, art. 18).

		dados.json: tudo o que o aplicativo mostra a você (perfil, vínculos com nutricionistas, dietas recebidas, registros de rotina, avaliações compartilhadas, questionários, consultas e avisos).
		dietas/ e avaliacoes/: os mesmos planos e avaliações em PDF.

		Este arquivo contém dados de saúde. Guarde-o em lugar seguro e não o compartilhe.
		""";
}
