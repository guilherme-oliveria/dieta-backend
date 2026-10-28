package com.br.dietamais.config;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * BE-35: carga de dados fictícios para desenvolvimento e prints. Só no perfil dev e com {@code dieta.dev.dados-exemplo=true}
 * (ex.: {@code ./gradlew bootRun --args=--dieta.dev.dados-exemplo=true}). Usa a própria API HTTP, então exercita as mesmas regras de produção.
 * Idempotente: se a nutri de exemplo já existe, não faz nada. Nenhum dado real.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "dieta.dev.dados-exemplo", havingValue = "true")
public class DadosExemploDev implements ApplicationRunner {
	static final String EMAIL_NUTRI = "nutri@exemplo.dev";
	static final String SENHA = "Exemplo de frase para desenvolvimento 2026";
	private static final Logger LOG = LoggerFactory.getLogger(DadosExemploDev.class);
	private final Environment ambiente;private final JdbcTemplate jdbc;private final JsonMapper mapper;
	private final HttpClient http = HttpClient.newHttpClient();
	private String base;

	public DadosExemploDev(Environment ambiente, JdbcTemplate jdbc, JsonMapper mapper) { this.ambiente = ambiente; this.jdbc = jdbc; this.mapper = mapper; }

	@Override public void run(ApplicationArguments args) throws Exception {
		if (jdbc.queryForObject("select count(*) from usuario where email=?", Integer.class, EMAIL_NUTRI) > 0) { LOG.info("Dados de exemplo já existem; nada a fazer."); return; }
		base = "http://localhost:" + ambiente.getProperty("local.server.port", "8080") + "/api/v1";
		chamar("POST", "/auth/cadastro-nutri", null, Map.of("nome", "Dra. Luciana Exemplo", "email", EMAIL_NUTRI, "senha", SENHA, "crn", "CRN-1 00000", "aceitouTermos", true, "versaoTermos", "2026-10"), 201);
		jdbc.update("update usuario set email_confirmado_em=now() where email=?", EMAIL_NUTRI);
		String token = entrar(EMAIL_NUTRI);
		var maria = criarPaciente(token, "Maria Exemplo", "maria@exemplo.dev");
		var joao = criarPaciente(token, "João Exemplo", "joao@exemplo.dev");
		criarPaciente(token, "Ana Exemplo", "ana@exemplo.dev");
		aceitarConvite(token, maria, "Maria Exemplo", "maria@exemplo.dev");
		convidar(token, joao); // fica com convite pendente
		var dieta = chamar("POST", "/pacientes/" + maria + "/dietas", token, dieta(token), 201);
		chamar("POST", "/dietas/" + dieta.path("id").asString() + "/publicar", token, null, Map.of("If-Match", "\"" + dieta.path("versao").asLong() + "\""), 200);
		var avaliacao = new HashMap<String, Object>();
		avaliacao.put("dataHora", java.time.Instant.now().minusSeconds(3600).toString()); avaliacao.put("pesoKg", new BigDecimal("68.4")); avaliacao.put("estaturaM", new BigDecimal("1.65"));
		avaliacao.put("perimetros", Map.of("cintura", new BigDecimal("78"), "quadril", new BigDecimal("98"))); avaliacao.put("parecer", "Avaliação fictícia de exemplo.");
		chamar("POST", "/pacientes/" + maria + "/avaliacoes", token, avaliacao, 201);
		LOG.info("Dados de exemplo criados. Nutri: {} / paciente: maria@exemplo.dev (mesma senha): {}", EMAIL_NUTRI, SENHA);
	}

	/** Dieta fictícia de cerca de 2510 kcal em 6 refeições, montada com o catálogo da nutri. */
	private Map<String, Object> dieta(String token) throws Exception {
		var grupos = new HashMap<String, JsonNode>();
		for (var g : chamar("GET", "/grupos-alimentos", token, null, 200).path("itens")) grupos.put(g.path("nome").asString(), g);
		var refeicoes = new ArrayList<Object>();
		refeicoes.add(refeicao("Café da manhã", "07:00:00", 0, item(token, grupos, "Pão", "2"), item(token, grupos, "Leite", "2"), item(token, grupos, "Fruta", "1")));
		refeicoes.add(refeicao("Lanche da manhã", "10:00:00", 1, item(token, grupos, "Fruta", "2"), item(token, grupos, "Castanha", "2")));
		refeicoes.add(refeicao("Almoço", "12:30:00", 2, item(token, grupos, "Arroz", "4"), item(token, grupos, "Feijão", "3"), item(token, grupos, "Carne ou ovo", "3"), item(token, grupos, "Vegetal refogado", "3"), item(token, grupos, "Gordura", "1")));
		refeicoes.add(refeicao("Lanche da tarde", "16:00:00", 3, item(token, grupos, "Pão", "1"), item(token, grupos, "Leite", "1"), item(token, grupos, "Fruta", "1")));
		refeicoes.add(refeicao("Jantar", "19:30:00", 4, item(token, grupos, "Sopa", "1")));
		refeicoes.add(refeicao("Ceia", "22:00:00", 5, item(token, grupos, "Leite", "1")));
		return Map.of("titulo", "Plano de exemplo (≈2510 kcal)", "refeicoes", refeicoes);
	}
	private Map<String, Object> refeicao(String nome, String horario, int ordem, Object... itens) { return Map.of("nome", nome, "horario", horario, "ordem", ordem, "itens", List.of(itens)); }
	private Map<String, Object> item(String token, Map<String, JsonNode> grupos, String grupo, String porcoes) throws Exception {
		var g = grupos.get(grupo);
		if (g == null) throw new IllegalStateException("Grupo ausente no catálogo: " + grupo);
		var alimento = chamar("GET", "/alimentos?tamanho=1&grupoId=" + g.path("id").asString(), token, null, 200).path("itens").get(0);
		return Map.of("alimentoId", alimento.path("id").asString(), "porcoes", new BigDecimal(porcoes));
	}
	private String criarPaciente(String token, String nome, String email) throws Exception {
		return chamar("POST", "/pacientes", token, Map.of("nome", nome, "email", email), 201).path("id").asString();
	}
	private String convidar(String token, String paciente) throws Exception {
		return chamar("POST", "/pacientes/" + paciente + "/convite", token, null, 201).path("codigo").asString();
	}
	private void aceitarConvite(String token, String paciente, String nome, String email) throws Exception {
		String codigo = convidar(token, paciente);
		var corpo = new HashMap<String, Object>();
		corpo.put("aceitouTermo", true);
		corpo.put("versaoTermo", ambiente.getProperty("dieta.consentimento.versao"));
		corpo.put("dispositivo", Map.of("id", UUID.randomUUID(), "plataforma", "ANDROID", "nomeAparelho", "Aparelho de exemplo"));
		corpo.put("cadastro", Map.of("nome", nome, "email", email, "senha", SENHA, "aceitouTermos", true, "versaoTermos", "2026-10"));
		chamar("POST", "/convites/" + codigo + "/aceitar", null, corpo, 200);
	}
	private String entrar(String email) throws Exception {
		var corpo = Map.of("email", email, "senha", SENHA, "dispositivo", Map.of("id", UUID.randomUUID(), "plataforma", "DESKTOP", "nomeAparelho", "Carga de exemplo"));
		return chamar("POST", "/auth/login", null, corpo, 200).path("accessToken").asString();
	}
	private JsonNode chamar(String metodo, String caminho, String token, Object corpo, int esperado) throws Exception { return chamar(metodo, caminho, token, corpo, Map.of(), esperado); }
	private JsonNode chamar(String metodo, String caminho, String token, Object corpo, Map<String, String> cabecalhos, int esperado) throws Exception {
		var pedido = HttpRequest.newBuilder(URI.create(base + caminho));
		cabecalhos.forEach(pedido::header);
		if (token != null) pedido.header("Authorization", "Bearer " + token);
		if (corpo != null) pedido.header("Content-Type", "application/json");
		pedido.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)));
		var r = http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
		if (r.statusCode() != esperado) throw new IllegalStateException("Carga de exemplo: " + metodo + " " + caminho + " respondeu " + r.statusCode());
		return r.body().isBlank() ? null : mapper.readTree(r.body());
	}
}
