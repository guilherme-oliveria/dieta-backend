package com.br.dietamais.config;

import java.math.BigDecimal;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	/** Senha usada só para criar as contas pela API (a política exige 10+ caracteres). */
	static final String SENHA = "Exemplo de frase para desenvolvimento 2026";
	/** Senha final das contas de exemplo, curta para facilitar o teste manual. Só existe no perfil dev. */
	static final String SENHA_TESTE = "teste123";
	private static final Logger LOG = LoggerFactory.getLogger(DadosExemploDev.class);
	private final Environment ambiente;private final JdbcTemplate jdbc;private final JsonMapper mapper;private final PasswordEncoder encoder;
	private final HttpClient http = HttpClient.newHttpClient();
	private String base;

	public DadosExemploDev(Environment ambiente, JdbcTemplate jdbc, JsonMapper mapper, PasswordEncoder encoder) { this.ambiente = ambiente; this.jdbc = jdbc; this.mapper = mapper; this.encoder = encoder; }

	@Override public void run(ApplicationArguments args) throws Exception {
		base = "http://localhost:" + ambiente.getProperty("local.server.port", "8080") + "/api/v1";
		if (jdbc.queryForObject("select count(*) from usuario where email=?", Integer.class, EMAIL_NUTRI) > 0) {
			completarHistoricoDaMaria();
			LOG.info("Dados de exemplo já existem; nada a fazer."); return;
		}
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
		lancarHistorico(token, maria);
		jdbc.update("update usuario set senha_hash=? where email like '%@exemplo.dev'", encoder.encode(SENHA_TESTE));
		LOG.info("Dados de exemplo criados. Nutri: {} / paciente: maria@exemplo.dev (mesma senha): {}", EMAIL_NUTRI, SENHA_TESTE);
	}

	/**
	 * Bases já criadas antes do histórico existir: entra como a nutri de exemplo (senha final) e completa a Maria. Se a senha já foi trocada
	 * ou a Maria não existe, ignora, porque é só conveniência de desenvolvimento.
	 */
	private void completarHistoricoDaMaria() {
		try {
			var ids = jdbc.queryForList("select f.paciente_id from ficha_paciente f where f.email=? and f.excluido_em is null", UUID.class, "maria@exemplo.dev");
			if (ids.isEmpty()) return;
			var maria = ids.get(0).toString();
			String token = entrar(EMAIL_NUTRI, SENHA_TESTE);
			lancarHistorico(token, maria);
		} catch (Exception e) { LOG.warn("Histórico de exemplo da Maria não foi completado: {}", e.getMessage()); }
	}

	/**
	 * Histórico de 6 meses da Maria para o gráfico de Evolução: 1 avaliação por semana (26 semanas, a mais recente de 7 dias atrás; a de agora já existe).
	 * Peso de 76 a 68,4 kg, cintura de 88 a 78 cm e quadril de 104 a 98 cm, com oscilação determinística e estatura fixa de 1,65 m (o IMC o servidor calcula).
	 * Idempotente: só lança se a Maria tiver menos de 10 avaliações.
	 */
	private void lancarHistorico(String token, String paciente) throws Exception {
		var existentes = chamar("GET", "/pacientes/" + paciente + "/avaliacoes", token, null, 200).path("itens");
		if (existentes.size() >= 10) return;
		var agora = java.time.Instant.now();
		int semanas = 26;
		for (int i = 0; i < semanas; i++) {
			int atras = semanas - i; // 26 semanas atrás ... 1 semana atrás
			double progresso = i / (double) semanas;
			double oscilacao = Math.sin(i * 1.7) * 0.45 + Math.cos(i * 0.9) * 0.25;
			var perimetros = new HashMap<String, Object>();
			perimetros.put("cintura", arredondar(88 - 10 * progresso + oscilacao * 0.8));
			perimetros.put("quadril", arredondar(104 - 6 * progresso + oscilacao * 0.5));
			var corpo = new HashMap<String, Object>();
			corpo.put("dataHora", agora.minus(java.time.Duration.ofDays(7L * atras)).toString());
			corpo.put("pesoKg", arredondar(76 - 7.6 * progresso + oscilacao));
			corpo.put("estaturaM", new BigDecimal("1.65"));
			corpo.put("perimetros", perimetros);
			corpo.put("parecer", "Avaliação semanal fictícia de exemplo.");
			chamar("POST", "/pacientes/" + paciente + "/avaliacoes", token, corpo, 201);
		}
	}
	private static BigDecimal arredondar(double v) { return BigDecimal.valueOf(v).setScale(1, java.math.RoundingMode.HALF_UP); }

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
	private String entrar(String email) throws Exception { return entrar(email, SENHA); }
	private String entrar(String email, String senha) throws Exception {
		var corpo = Map.of("email", email, "senha", senha, "dispositivo", Map.of("id", UUID.randomUUID(), "plataforma", "DESKTOP", "nomeAparelho", "Carga de exemplo"));
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
