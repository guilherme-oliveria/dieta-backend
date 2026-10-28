# Segurança do backend (BE-31)

Revisão do backend contra o OWASP ASVS nível 2 e as decisões de endurecimento do MVP. O que está **feito** vale hoje no `develop`; o que está **pendente** tem dono e prazo no fim do documento.

## Controles implementados em BE-31

| Controle | Como | Configuração |
| --- | --- | --- |
| Limite geral de taxa | 300 requisições por minuto por usuário autenticado; excedeu: `429 TAXA_EXCEDIDA` com `Retry-After`. Janela deslizante em memória (50 mil usuários no máximo, recusa segura ao lotar). Login e convites mantêm os limites por IP (10/min) | `TAXA_USUARIO_POR_MINUTO` (0 desliga) |
| CORS por variável | Só as origens listadas, HTTPS exatas (ou `http://localhost` em dev); métodos e cabeçalhos explícitos; sem coringa. Vazio = nenhuma origem de navegador | `CORS_ORIGENS` (vírgula) |
| HSTS | `max-age=31536000; includeSubDomains` quando o ambiente termina o TLS (atrás do Caddy). Desligado em dev | `HSTS_HABILITADO=true` |
| Cabeçalhos | `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` nas respostas de dado | fixo |
| Versão mínima do app | Cabeçalho `X-App-Versao` abaixo do mínimo: `426 VERSAO_APP_OBSOLETA` em `/api/v1/**` (menos webhooks e links públicos). Sem o cabeçalho (site, curl) não bloqueia | `APP_VERSAO_MINIMA` (ex.: `1.4.0`; vazio desliga) |
| IP real atrás do proxy | `SERVER_FORWARD_HEADERS_STRATEGY=native`: o Tomcat só aceita `X-Forwarded-For` de proxies internos (faixas privadas e loopback). Os limites por IP usam esse endereço | `compose.prod.yaml` |

## Revisão ASVS nível 2 (resumo)

| Capítulo | Situação |
| --- | --- |
| V2 Autenticação | Senha com política (RN-CON-02) e bcrypt; bloqueio persistente por e-mail (RN-CON-03); limite por IP; e-mail confirmado antes do convite; redefinição por token de uso único (1 h) que revoga sessões. **Pendente:** segundo fator (MFA) para nutri e, principalmente, para o ADMIN |
| V3 Sessão | Access RS256 curto + refresh rotativo com revogação da família no reuso; logout por dispositivo; conta desativada derruba o token na hora (checagem a cada requisição) |
| V4 Controle de acesso | Backend decide tudo; isolamento por dono em toda consulta clínica com teste (`TesteIsolamento`, 404 para alheio); `FiltroContextoUsuario` valida perfil e vínculo; o ADMIN só entra em `/admin/**` e nunca vê dado clínico (teste de 403 em todas as rotas clínicas) |
| V5 Validação | DTOs com Bean Validation, JSON desconhecido tratado, `If-Match` em escrita, intervalos clínicos validados (RN-AVA-03), upload de logo conferido pelos bytes e limitado a 512 KB |
| V6 Criptografia | Senhas em hash; segredos só em variável de ambiente; payload de e-mail cifrado enquanto pendente; links de arquivo assinados com HMAC (local) ou SigV4 (S3), validade de minutos; token de exportação guardado só como hash |
| V7 Erros e log | ProblemDetail com código estável, sem stack trace; log sem senha, token, e-mail nem dado de saúde; `log_acesso` (5 anos) e `log_admin` (somente inserção) |
| V8 Proteção de dados | Dado de saúde fora de push/e-mail; exportação e exclusão de conta (LGPD); PDFs apagados após o link expirar |
| V9 Comunicação | HTTPS no Caddy, HSTS; CORS restrito |
| V12 Arquivos | Download só por URL assinada; chave de arquivo validada (sem `..`); tipo conferido |
| V13 API | Versionada (`/api/v1`), idempotência nas escritas, limites de taxa, cabeçalhos de segurança |
| V14 Configuração | Dependências travadas em `gradle`; sem segredo no Git; perfis dev/prod separados |

## Row Level Security do Postgres (avaliação)

**Decisão: não entra no MVP.** A segunda barreira seria `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` com política por `current_setting('dieta.nutricionista_id')`, definida com `SET LOCAL` no início de cada transação.

- **Custo:** o acesso é por `JdbcTemplate` e JPA em conexões de um pool; `SET LOCAL` exige transação em *toda* leitura, inclusive os jobs (sync, exportação, ciclo de assinatura) que hoje leem entre dono. Qualquer consulta fora do padrão passa a retornar vazio em silêncio, o que é difícil de depurar.
- **Ganho hoje:** o isolamento já tem teste automatizado por endpoint e toda consulta clínica filtra por dono. O risco que a RLS cobre (uma consulta nova esquecer o filtro) é coberto pela regra de revisão do QA e pelo `TesteIsolamento` obrigatório.
- **Quando reavaliar:** antes de abrir o site para mais de uma organização ou se houver um segundo cliente de dados (BI, suporte). Plano: usuário do banco sem `BYPASSRLS` para a aplicação, políticas por `nutricionista_id`/`paciente_id`, usuário separado para jobs, tudo atrás de um teste que tenta ler dado alheio direto no SQL.

## Automação no GitHub (para o Guilherme aplicar)

O ambiente de desenvolvimento do agente não escreve em `.github/`. Os arquivos prontos estão em `ops/github/`; copie para `.github/` do repositório:

- `ops/github/dependabot.yml`: atualização semanal de Gradle, GitHub Actions e Docker.
- `ops/github/workflows/seguranca.yml`: revisão de dependências em PR (`dependency-review-action`), CodeQL para Java e varredura de segredos (`gitleaks`).

## Pendências conhecidas

1. **MFA do ADMIN e da nutri** (ASVS V2.8): avaliar TOTP antes de abrir o beta para mais de uma clínica.
2. **Limites em memória por processo:** com mais de uma instância da API, mover os contadores para o Postgres ou um proxy (Caddy `rate_limit`). Com uma instância (VPS única) não há diferença.
3. **Varredura de dependências** só roda depois que o workflow acima for copiado.
