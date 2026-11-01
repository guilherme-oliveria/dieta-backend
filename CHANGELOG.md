# Changelog — dieta-backend

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), versões [SemVer](https://semver.org/lang/pt-BR/). Cada versão é um marco de `docs/arquitetura/plano-de-execucao.md` (seção 3).

> **Estado em 29/10/2026:** ainda não existe tag nem `release/`. Todo o conteúdo abaixo já está na `develop`; os marcos foram sendo entregues em ondas e entraram juntos. Os números de versão abaixo agrupam o que cada marco entrega. A decisão de como cortar as `release/` está em "Para o Guilherme" no final.

## [Não lançado] — pendente para fechar 1.0.0 e 1.1.0

- BE-32b Implantação real na VPS (domínio, DNS, `.env.prod`, backup externo, primeiro `ADMIN`). Depende do Guilherme; o ensaio local está em `docs/operacao/ensaio-producao.md`.
- **Termos de uso** (`docs/legal/terms-of-use.md` está como 1.0.0 RASCUNHO): enquanto não forem aprovados e publicados o cadastro continua bloqueado (BE-36).
- Texto do termo de consentimento do paciente (`CONSENTIMENTO_VERSAO` e `CONSENTIMENTO_TEXTO`) e política de privacidade.
- Provedor de e-mail transacional, projeto Firebase (push) e conta do gateway de pagamento (Asaas): o código está pronto e desligado sem as credenciais.

## [1.3.0] — M9: necessidades e protocolos (em andamento)

- BE-50b PDF da lista de substituição publicada em quatro versões (pequena, média, grande e contagem de carboidratos), com escala de gramas e medidas caseiras, valores provisórios por grupo e aviso de referências ausentes. Rotas `/dietas/{id}/pdf-lista` e `/eu/dietas/{id}/pdf-lista`, link assinado por 10 minutos, isolamento e auditoria existentes preservados.

- BE-43a/b GEB, GET (FAO 1985 e 2004, IOM, RDI e MET), VET e metas de macronutrientes, gravados na avaliação (`energia`, `energiaResultado`). BE-44 Tabela de MET. BE-45 Tabelas DRI. Endpoints `GET /referencias/atividades-met` e `/referencias/dri`.
- BE-51 Marcadores de restrição nos alimentos (`CONTEM_LACTOSE`, `CONTEM_GLUTEN`, `ALTO_SODIO`, `ACUCAR_ADICIONADO`, `ALTO_POTASSIO`; changeset 042): sugestão inicial por nome no catálogo (SUPOSIÇÃO G-15), editável por nutri e filtro `semMarcador` em `GET /alimentos`.
- BE-52a Modelos de dieta (changeset 043 `modelo_dieta`): `GET/POST /eu/nutri/modelos-dieta`, `GET/PUT/DELETE /{id}` com `If-Match`, `/duplicar`, `/de-dieta/{dietaId}` (salvar dieta como modelo) e `POST /pacientes/{id}/dietas/de-modelo/{modeloId}`, que cria um rascunho com as porções escaladas pelo VET (0,5 em 0,5) e avisa o que saiu por restrição. Modelos do sistema são somente leitura (403 ao editar), os da nutri são isolados (404) e entram no `/sync` como `modelosDieta`. BE-52b: a restrição do modelo vira `restricoes` da dieta (changeset 044) e, ao publicar, a lista de substituição do snapshot sai sem os alimentos marcados (RN-MOD-03). BE-53 Modelos iniciais do sistema (changeset 045): Sem restrições, Hipertensão (DASH), Intolerância à lactose, Diabetes tipo 2, Dislipidemia, Doença celíaca, Gestante, Obesidade e Doença renal crônica, em porções por grupo e com restrições por marcador. O conteúdo clínico é SUPOSIÇÃO G-15: a nutri precisa validar antes do uso.
- BE-56b Exames por avaliação (changeset 046 `avaliacao.exames`): `exames` na gravação e na resposta da avaliação, classificados pelo servidor com o catálogo da faixa etária (RN-EXA-01); o paciente só recebe com `compartilhamento.exames`. Mudança de contrato opcional (campo novo); copiar `contrato-fase-7` para o mobile.
- BE-56c `faixas` em cada exame numérico da avaliação (limites resolvidos para o sexo, a idade e a fase do paciente), para a régua de faixa do app. Campo novo opcional; copiar `contrato-fase-7` para o mobile.
- BE-49d Peso ajustado nos "por kg" (RN-CAL-05: adulto com IMC 30 ou mais, `pesoReferenciaKg` em `energiaResultado`), `GET /pacientes/{id}/dietas/referencia-avaliacao` (peso, metas e gestante/lactante da avaliação mais recente) e adequação DRI com gestação e lactação. Vetores `peso-referencia.json`.
- BE-46 PDF da avaliação no layout da nutri, com 4 gráficos (peso × faixa teórica, perímetros, dobras e rosca da composição), energia e metas, referências e rodapé com o CRN.
- BE-47a Tabela TACO (597 alimentos, 29 nutrientes por 100 g) com busca em `GET /api/v1/referencias/taco` e `/taco/{numero}`; fonte citada na resposta. Nutriente não analisado fica ausente (nunca zero); traço vira 0 e aparece em `tracos`.
- BE-54a Curvas de crescimento da OMS (peso, estatura e IMC por idade; 736 linhas) em `GET /api/v1/referencias/oms`, copiadas das abas P.I., E.I. e I.M.C. da planilha. Nas faixas de 5 a 10 anos as colunas 0,1° e 10° da planilha andavam em degraus anuais e em 53 linhas o 3° passava do 10° (G-16); foram refeitas a partir das tabelas oficiais da OMS de 2007 (432 células), e o teste agora exige percentis crescentes.
- BE-54b Percentis de Frisancho (1981) de 2 a 9 anos (CB, DCT, DCSE, AMBc e AGB) em `GET /api/v1/referencias/frisancho`. Na planilha, o AMBc masculino de 4 anos tem o percentil 5 (1297) acima do 10 (1264) (G-16); mantido como está.
- BE-56a Catálogo de exames da avaliação bioquímica (crianças, adolescentes e adultos: 395 exames e 2.223 degraus) com classificação em `GET /api/v1/referencias/exames` e `/exames/{codigo}/classificar`. Os degraus saem das fórmulas da planilha e foram conferidos contra elas em mais de 160 mil combinações. Na planilha de 10 a 17 anos a referência do sexo (e a data de nascimento, em alguns exames) está quebrada (`#REF!`) em 30 fórmulas; foi tratada como sexo e idade (G-16).
- BE-57a Calculadoras da anamnese (pressão arterial, álcool, sal, óleo e duração do exercício) com vetores `contrato/calculos/anamnese.json`. A pressão com sistólica a partir de 140 e diastólica abaixo de 90 passa a ser hipertensão sistólica isolada (a macro da planilha só chegava a esse ramo com 180 ou mais).
- BE-47b Porções por grupo da dieta (`GET /api/v1/referencias/porcoes`): 31 porções em 15 grupos (entram Vegetal cru ou cozido, Embutido e Bebida, copiados também para as nutris que já existem), com medida caseira, marca do Guia Alimentar e até 34 nutrientes por porção. Os valores são as médias dos alimentos de cada grupo na planilha "Dieta sem restrições" e são provisórios; a resposta traz a fonte para citar no app e no PDF. Produtos de marca ficam de fora até a origem ser confirmada.
- BE-C8 Contrato da dieta por porções, metas e modelos (`docs/arquitetura/contrato-fase-8.md`): campos novos e opcionais na dieta (porções por grupo, metas, alternativas, itens à vontade, rodapé, produtos), totais de micronutrientes com `incompleto` e adequação à DRI, códigos `ITEM_INVALIDO` e `PORCAO_INVALIDA`, regras RN-DIE-10 a RN-DIE-13 e RN-CAL-07 e vetores `contrato/calculos/dieta-porcoes.json`.
- BE-49a Dieta por porções do grupo: `SalvarDietaRequisicao` aceita `porcoes` (opção por grupo), `metas` (VET e % de macros), `rodape`; refeição `tipo` PRINCIPAL ou ALTERNATIVA (fora do total do dia, com `diferencaParaPrincipal`); item por `grupoId`, `aVontade` ou `textoLivre`. A resposta traz `saldoMetas` no total do dia. Campos de item por alimento (`alimentoId`, `nomeAlimento`, `medidaCaseira`, `gramas`, `porcoes`) passam a ser opcionais na resposta, porque item por grupo, à vontade e texto livre não os têm. Códigos novos `ITEM_INVALIDO` e `PORCAO_INVALIDA`. Micronutrientes e adequação ficam para a BE-49b.
- BE-49b Totais da dieta com `micronutrientes` (por item, refeição e dia) e `incompleto` (códigos em que algum item não informa o nutriente; nunca zero por falta de dado), e `adequacao` do dia contra as DRI do paciente (sexo e idade da ficha; sem eles não há adequação). Classes ABAIXO (< 90%), ADEQUADO e ACIMA (> 110%), limites provisórios. Gestação e lactação e o peso ajustado ficam para a BE-49d.
- BE-50a PDF da dieta com meta e saldo, coluna Substituições ("1 porção do grupo X"), itens à vontade e texto livre, refeições alternativas, rodapé e a fonte dos valores por porção. O PDF da lista de substituição fica para a BE-50b.
- BE-55a Calculadoras de crianças e adolescentes (`avaliacoes/dominio/infantil`): peso ao nascer, adequação (CMB/DCT/DCSE), AMBc e AGB, Slaughter, classes do % de gordura (Deurenberg), classes de peso, estatura e IMC por idade, GEB/GET IOM e GET RDI, com vetores `contrato/calculos/infantil.json`. Correções sobre a planilha (G-16): as faixas A e EP do CMB não se sobrepõem mais; a classe da AMBc usa o P90 (a macro citava variável indefinida); o Slaughter de menino negro de 10 e 11 anos usa a faixa certa (a macro comparava `idade > 12`).

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
