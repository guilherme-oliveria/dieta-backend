# Contrato da fase 6 — assinatura, PDFs, LGPD e admin (BE-C6)

Cópia oficial no backend. As seções entram junto com a implementação de cada tarefa (BE-26 a BE-30). Erros seguem o `ProblemDetail` de [integracao.md](integracao.md).

## Assinatura e checkout (BE-26)

O app **não vende nada** (RN-ASS-06): ele só pede o link do checkout hospedado do gateway (Asaas `[SUPOSIÇÃO S3]`) e abre no navegador externo. O status da assinatura só muda por webhook validado, job diário ou ação do admin (RN-ASS-07).

### `GET /api/v1/assinatura` (nutri)

Funciona mesmo com a assinatura inativa (é só leitura).

```json
{
  "organizacaoId": "…",
  "plano": { "codigo": "TESTE", "nome": "Teste", "precoMensal": 0.00, "limitePacientes": 5 },
  "status": "TESTE",
  "pacientesOcupados": 2,
  "fimPeriodoEm": "2026-11-27T12:00:00Z",
  "carenciaAte": null,
  "pagamentoDisponivel": true,
  "planosDisponiveis": [ { "codigo": "BASICO_20", "nome": "Básico", "precoMensal": 29.90, "limitePacientes": 20 } ]
}
```

- `carenciaAte` só vem em `EM_ATRASO` (`fimPeriodoEm` + 7 dias): depois dela a nutri fica só leitura (RN-ASS-04).
- `pagamentoDisponivel=false` quando o servidor está sem chave do gateway; o app esconde o botão de contratar.

### `POST /api/v1/assinatura/checkout` (nutri)

Corpo `{ "plano": "BASICO_20" }`; resposta `{ "url": "https://…" }`. Chamar de novo para o mesmo plano devolve o link da cobrança em aberto.

| Situação | Resposta |
| --- | --- |
| Plano inexistente, inativo | `404 RECURSO_NAO_ENCONTRADO` |
| Plano gratuito (`TESTE`) | `400 FORMATO_INVALIDO` |
| Assinatura já `ATIVA` | `409 ASSINATURA_JA_ATIVA` (RN-ASS-06) |
| Gateway sem chave ou fora do ar | `503 PAGAMENTO_INDISPONIVEL` (RN-ASS-06) |
| Paciente | `403 PERFIL_INVALIDO` |

### `POST /api/v1/webhooks/pagamento` (público, só o gateway)

Exige o cabeçalho `asaas-access-token` igual a `ASAAS_WEBHOOK_TOKEN` (comparação em tempo constante); sem ele, ou com servidor sem token configurado, `401`. Corpo inválido: `400 FORMATO_INVALIDO`. O evento é gravado em `evento_pagamento` com `evento_id` único: repetido responde `200` sem efeito.

| Evento do gateway | Efeito |
| --- | --- |
| `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED` | `ATIVA`; troca para o plano contratado; `fimPeriodoEm` = vencimento + 1 mês |
| `PAYMENT_OVERDUE` | `ATIVA` → `EM_ATRASO`; `fimPeriodoEm` = vencimento em atraso (carência de 7 dias, RN-ASS-04) |
| `SUBSCRIPTION_DELETED`, `SUBSCRIPTION_INACTIVATED` | `ATIVA`/`EM_ATRASO` → `CANCELADA`; em teste só solta o vínculo com o gateway |
| recusa de cartão / análise de risco | só avisa |
| outros | ignorados (`200`) |

Cada mudança relevante gera um aviso `ASSINATURA` (genérico, RN-NOT-02) para as nutris da organização.

Configuração (somente variáveis de ambiente): `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN`, `ASAAS_URL_BASE` (padrão: sandbox).

## Ciclo de vida da assinatura (BE-27)

Sem endpoint novo. Um job de hora em hora (`dieta.assinatura.intervalo-ciclo`) mantém os status coerentes e avisa a nutri (aviso `ASSINATURA`, genérico, push e central; e-mail fica para depois):

| Situação | Efeito |
| --- | --- |
| `TESTE` com `fimPeriodoEm` vencido | `EXPIRADA` (RN-ASS-02) + aviso |
| `TESTE` a 3 dias do fim (`dieta.assinatura.aviso-fim-teste`) | aviso, uma vez |
| `ATIVA` sem renovação por mais de 3 dias (`dieta.assinatura.tolerancia-webhook`, webhook perdido) | `EM_ATRASO` + aviso |
| `EM_ATRASO` além da carência de 7 dias (RN-ASS-04) | aviso de bloqueio, uma vez por vencimento; escrita já recusa com `ASSINATURA_INATIVA` |

Downgrade (RN-ASS-08): com plano de limite menor que os pacientes ativos, novos convites ficam bloqueados (`LIMITE_PACIENTES_ATINGIDO`) e nenhum vínculo é encerrado. O paciente nunca é afetado (RN-ASS-05).
