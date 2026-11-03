# Handoff para o Codex — dieta-backend

Escrito em 01/11/2026 pelo agente executor (Claude) ao passar as tarefas restantes das ondas 20 a 23 para o Codex. Continue **exatamente** daqui.

## Como trabalhar

1. Leia `AGENTS.md` e, nesta ordem: `docs/arquitetura/plano-de-execucao.md` (seção 3, marcos), `plano-de-acao.md` (linhas das ondas 17 a 23), `plano-melhorias-planilhas.md` (a origem das tarefas; cópia em `/mnt/project-files/planos/` do projeto), `regras-de-negocio.md` (RN-DIE-10 a 13, RN-MOD-01 a 03, RN-EXA-01, RN-CAL-04/05/07), `contrato-fase-7.md` e `contrato-fase-8.md`, `backend.md`, `integracao.md`.
2. Uma tarefa por PR, branch `feature/BE-NN-descricao` a partir da `develop`, PR para a `develop` com o ID no título. Commits em português no imperativo. Marque `[x]` no `plano-de-acao.md` no mesmo PR e atualize o `CHANGELOG.md` (seção da versão em andamento).
3. Antes do PR: `JAVA_HOME=<jdk25> ./gradlew build`. Sem Docker, os testes Testcontainers não sobem: escreva-os assim mesmo e confie no CI. Se o Maven Central responder 429, use `--max-workers=1`.
4. Revisão: o fluxo de QA do `AGENTS.md` vale (você abre o PR e não aprova nem mescla o próprio trabalho). Se o Guilherme tiver dispensado o QA, ele avisa no prompt.
5. Mudou contrato ou regra? Escreva no PR para espelhar `contrato-fase-*`, `regras-de-negocio` e `integracao` nas cópias do `dieta-mobile` (oficial é esta).
6. **Release:** cada marco vira `release/X.Y.Z` a partir da `develop` (M8 = 1.2.0 avaliação do adulto, M9 = 1.3.0 dieta por porções e modelos, M10 = 1.4.0 crianças, exames e anamnese). Quando todas as tarefas de um marco estiverem `[x]`: abra `release/X.Y.Z`, ajuste versão e `CHANGELOG`, rode o build e abra o PR `release/X.Y.Z → main` para o Guilherme aprovar. **Nunca** faça merge na `main` nem edite `.github/`.

## O que já está implementado (na `develop`)

- Avaliação do adulto e necessidades: BE-43a/b, 44, 45, 46 (PDF da avaliação), BE-49d (peso ajustado).
- Referências: TACO oficial (BE-47a), porções por grupo (BE-47b), OMS (BE-54a), Frisancho (BE-54b), catálogo de exames (BE-56a).
- Dieta por porções (BE-C8, BE-49a/b/c/d), PDF da dieta (BE-50a), marcadores (BE-51), modelos de dieta (BE-52a/b) e 9 modelos iniciais (BE-53).
- Exames por avaliação (BE-56b, changeset 046, RN-EXA-01) e `faixas` por paciente (BE-56c).
- Calculadoras: infantil (BE-55a, `avaliacoes/dominio/infantil`) e anamnese (BE-57a, `questionarios/dominio/anamnese`), ambas só como funções puras com vetores em `contrato/calculos/`.
- Próximo changeset livre: **047**.

## O que falta (nesta ordem)

### 1. BE-50b — PDF da lista de substituição
- Layout: artboards `PDF-Lista-Pequena`, `PDF-Lista-Media`, `PDF-Lista-Grande` e `PDF-Lista-Carboidratos` em `dieta-mobile/docs/design/artboards/*.dc.html` (HTML estático; abra no navegador ou leia o texto) e a seção 9 de `dieta-mobile/docs/design/padrao-ui.md`. Cabeçalho "D+", nutri e CRN, paciente e data, aviso "Troque um alimento por outro do mesmo grupo…", um bloco por grupo ("Pães — 1 porção = N kcal") com a tabela Alimento | Medida caseira | g (a versão Carboidratos acrescenta a coluna "Carb. (g)" e o exemplo de soma por refeição), exemplo no fim e rodapé "Gerado pelo Dieta+ · não substitui a orientação da sua nutricionista".
- Onde: módulo `relatorios` (`ServicoPdf`, `GeradorPdf`, `PdfController`). Endpoints novos: `GET /dietas/{id}/pdf-lista?tamanho=PEQUENA|MEDIA|GRANDE|CARBOIDRATOS` (nutri) e `GET /eu/dietas/{id}/pdf-lista?tamanho=...` (paciente), mesmo retorno `PdfResposta` (link de 10 min). Fonte dos dados: o **snapshot publicado** (`listaSubstituicao` congelada, RN-DIE-03) mais `ConsultarAlimentosParaDieta` (kcal e carboidrato do grupo por porção, `grupoNome`) com o `nutricionista().id()` do snapshot, e `ConsultaPorcoes` (opções de porção de cada grupo). Conferir `ArquiteturaTest`: `relatorios` só pode usar o `api/` dos outros módulos.
- Decisão tomada (não bloqueie por ela, deixe fácil de mudar): o tamanho escolhe a opção de porção do grupo por kcal crescente (PEQUENA = a menor, GRANDE = a maior, MEDIA = a do meio; grupo com uma opção só repete a mesma nas 3). O fator de escala de cada alimento = kcal da opção ÷ kcal da porção-base do alimento; gramas arredondam para inteiro; a **medida caseira também escala, arredondando para ¼** (parse do número inicial, aceita ½ ¼ ¾ e decimais; sem número no início, prefixe "N × "). Carboidrato da coluna "Carb. (g)" = carboidrato da porção do grupo × fator, **dado por grupo** (as médias da planilha, provisórias: cite `PorcoesGrupos.FONTE`). Nunca invente valor ausente (RN-CAL-04): sem opção para o grupo, omita o grupo e avise no rodapé.
- Testes: função pura de escala da medida caseira (¼, ½, 1, 1½, "2 colheres", "½ unidade", sem número) em `relatorios/aplicacao` ou `dominio`; `PdfApiTest` (isolamento: dieta de outra nutri = 404; paciente só a dele); OpenAPI anotado; texto do PDF contém os grupos e a coluna Carb. só na versão de carboidratos.

### 2. BE-57b — ligar a anamnese ao questionário
`questionarios`: código na pergunta, valores derivados pelas calculadoras do BE-57a (PA, álcool, sal, óleo, duração) e modelo padrão "Anamnese completa". Artboards: `A-Anamnese-Estruturada` e `P-Anamnese`. Leia `plano-melhorias-planilhas.md` (seção da anamnese). Mudança de contrato: campos novos opcionais; espelhar no mobile.

### 3. BE-55b — avaliação infantil e adolescente
Ligar as calculadoras do BE-55a: entrada (peso ao nascer, braço, dobras), cortes da OMS (`referencia_oms`) e Frisancho, gravação na avaliação (changeset 047), endpoint e resposta com classes e percentil/Z. Artboards: `A-Avaliacao-Infantil` e `P-Curva-Crescimento`. Pendência **G-16**: o AMBc masculino de 4 anos tem p5 > p10 na planilha; mantido sem tabela oficial, não corrija sozinho.

### Bloqueadas (não faça)
- **BE-48** produtos e suplementos: falta a origem dos 411 produtos (depende do Guilherme).
- **BE-32b** implantação na VPS: depende do Guilherme.

## Para o relatório final
**G-15** (marcadores de restrição e os 9 modelos iniciais precisam da validação da nutri Luciana) e **G-16** (Frisancho AMBc masc. 4 anos). Diga também que os docs comuns foram espelhados no mobile.
