# Changelog — dieta-backend

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), versões [SemVer](https://semver.org/lang/pt-BR/). Cada versão é um marco de `docs/arquitetura/plano-de-execucao.md` (seção 3).

> **Estado em 29/10/2026:** ainda não existe tag nem `release/`. Todo o conteúdo abaixo já está na `develop`; os marcos foram sendo entregues em ondas e entraram juntos. Os números de versão abaixo agrupam o que cada marco entrega. A decisão de como cortar as `release/` está em "Para o Guilherme" no final.

## [Não lançado] — pendente para fechar 1.0.0 e 1.1.0

- BE-32b Implantação real na VPS (domínio, DNS, `.env.prod`, backup externo, primeiro `ADMIN`). Depende do Guilherme; o ensaio local está em `docs/operacao/ensaio-producao.md`.
- **Termos de uso** (`docs/legal/terms-of-use.md` está como 1.0.0 RASCUNHO): enquanto não forem aprovados e publicados o cadastro continua bloqueado (BE-36).
- Texto do termo de consentimento do paciente (`CONSENTIMENTO_VERSAO` e `CONSENTIMENTO_TEXTO`) e política de privacidade.
- Provedor de e-mail transacional, projeto Firebase (push) e conta do gateway de pagamento (Asaas): o código está pronto e desligado sem as credenciais.

## [1.3.0] — M9: necessidades e protocolos (em andamento)

- BE-43a/b GEB, GET (FAO 1985 e 2004, IOM, RDI e MET), VET e metas de macronutrientes, gravados na avaliação (`energia`, `energiaResultado`). BE-44 Tabela de MET. BE-45 Tabelas DRI. Endpoints `GET /referencias/atividades-met` e `/referencias/dri`.
- BE-46 PDF da avaliação no layout da nutri, com 4 gráficos (peso × faixa teórica, perímetros, dobras e rosca da composição), energia e metas, referências e rodapé com o CRN.
- BE-57a Calculadoras da anamnese (pressão arterial, álcool, sal, óleo e duração do exercício) com vetores `contrato/calculos/anamnese.json`. A pressão com sistólica a partir de 140 e diastólica abaixo de 90 passa a ser hipertensão sistólica isolada (a macro da planilha só chegava a esse ramo com 180 ou mais).

## [1.2.0] — M8: avaliação de adulto completa

- BE-C7 Contrato da fase 7 (avaliação ampliada, resultados, protocolos) com vetores de teste da planilha (`contrato/calculos/adulto.json`, iguais no mobile). BE-39 Tabelas de referência do adulto. BE-40 Motor de cálculo antropométrico do adulto (IMC, gordura, composição, peso para o % desejado; dado ausente vira motivo, nunca zero).
- BE-41 Avaliação ampliada: campos novos opcionais, protocolo usado e resultados gravados com o texto-resumo; compartilhamento por seção (RN-RES-01).
- BE-42 "Meus protocolos" da nutri (`/api/v1/nutri/protocolos`, sincronizado; padrão 7 dobras e peso magro).

## [1.1.0] — M7: web no navegador

- BE-37 Modo navegador: `X-Plataforma: WEB`, cookie `HttpOnly` do refresh (`Secure`, `SameSite=Strict`, `Path=/api/v1/auth`), conferência de `Origin` com `WEB_ORIGEM`, versão mínima própria da web (`APP_VERSAO_MINIMA_WEB`) e CORS liberando `X-Plataforma`.
- BE-38 Leitura da rotina do paciente para a web (`GET /eu/rotina`).

## [1.0.0] — M6: assinatura, PDFs, LGPD e produção

- BE-26 Gateway de pagamento (Asaas) e webhook.
- BE-27 Ciclo de vida da assinatura (teste, carência, bloqueio).
- BE-28 PDFs da dieta e da avaliação (link assinado, apagados sozinhos).
- BE-29a Exportação de dados e BE-29b exclusão de conta (LGPD).
- BE-30 Administração da plataforma.
- BE-31 Endurecimento (limites por usuário e IP, HSTS, CORS exato, `426 VERSAO_APP_OBSOLETA`).
- BE-32 Deploy na VPS: `compose.prod.yaml` (app, Postgres 18, Caddy com HTTPS e web na mesma origem, backup diário criptografado), `ops/backup.sh`, `ops/testar-restauracao.sh`, workflow de deploy por tag.

## [0.5.0] — M5: edição offline com conflitos

- BE-25 Sync de escrita com conflito (`409` por item no `/sync`).

## [0.4.0] — M4: avaliação, anamnese, agenda e push

- BE-19 Avaliações (IMC). BE-20 Evolução.
- BE-21 Modelos de questionário. BE-22 Envio e resposta.
- BE-23 Consultas. BE-24 Push (FCM) e jobs de lembrete, sem dado clínico no texto. BE-34 Central de avisos.

## [0.3.0] — M3: beta com a nutri real

- BE-15 Rotina do paciente e sync de escrita (check-in, água, peso). BE-16 Adesão.
- BE-17 Feed `.ics`. BE-18 Dispositivos e preferências de notificação.
- BE-32a Deploy de beta.

## [0.2.0] — M2: dieta montada e publicada

- BE-09 Catálogo base de alimentos. BE-10 Lista de substituição da nutri e do paciente.
- BE-11 Rascunho de dieta. BE-12 Publicar e duplicar. BE-13 Dieta do paciente. BE-14 Fontes de sync de alimentos e dietas.
- BE-33 Perfil da nutri. BE-35 Dados de exemplo (só no perfil `dev`).

## [0.1.0] — M1: conta, paciente e convite

- BE-01 Base transversal (ProblemDetail com `codigo`, idempotência, OpenAPI). BE-02 Cadastro da nutri. BE-03 Login e tokens JWT. BE-04 E-mail: confirmação e senha.
- BE-05 `UsuarioAtual`, isolamento por nutri (dado de outra nutri = `404`) e auditoria. BE-06 Pacientes. BE-07 Convite (código de 8 caracteres) e aceite com consentimento.
- BE-08 Sync de leitura. BE-36 Termos de uso.

## Variáveis de ambiente

A tabela completa (obrigatórias, opcionais e padrões) está em `docs/operacao/configuracao-producao.md`; o modelo é `.env.prod.example`. Valores que mudam o comportamento do beta: `EMAIL_URL_CONVITE` (precisa ser `https://…` para o app aceitar o link do convite), `WEB_ORIGEM` e `CORS_ORIGENS` (mesma origem do site), `APP_VERSAO_MINIMA` e `APP_VERSAO_MINIMA_WEB`.

## Para o Guilherme

Como os marcos já estão misturados na `develop`, `release/0.1.0` a `release/0.5.0` não podem ser cortadas no ponto exato de cada marco sem reescrever histórico. Opções: **(a)** uma única `release/1.1.0` agora, com este CHANGELOG, marcando 0.1.0–1.1.0 como histórico; **(b)** tags retroativas nos merges de cada onda (`git log --merges` mostra os PRs), sem `release/`. Recomendo (a) quando os termos e a VPS estiverem prontos, porque a 1.0.0 é a publicação nas lojas e hoje ela está bloqueada por contas e termos. Aprovar o PR `release/X.Y.Z → main` e criar a tag é só seu (G-09).
