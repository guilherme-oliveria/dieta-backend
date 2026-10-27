# Contrato da fase 3 — rotina do paciente e envio do sync (BE-C3 + BE-15)

Cópia oficial no backend. Mobile e frontend acompanham este contrato. A partir da BE-15 os contratos "BE-Cn" deixam de ter stubs 501 separados: cada tarefa publica o DTO, o OpenAPI e a implementação juntos, e o contrato é validado pelos testes de API da própria tarefa.

Todos os caminhos começam em `/api/v1`. Bearer obrigatório, perfil PACIENTE.

## Endpoints (BE-15)

| Método/caminho | Requisição | Sucesso |
| --- | --- | --- |
| POST /eu/checkins | `{id?, dietaId, refeicaoId, data, status, trocas?, registradoEm}` | 201 `CheckinResposta` |
| POST /eu/agua | `{id?, ml, registradoEm}` (1 a 5000 ml) | 201 `AguaResposta` |
| POST /eu/pesos | `{id?, pesoKg, registradoEm}` (20 a 400 kg, até 2 casas) | 201 `PesoResposta` |
| POST /sync | `{mutacoes: [...]}` (1 a 200) | 200 `{resultados: [...]}` |

`status` do check-in: `FEITO`, `TROCADO`, `PULADO`. `trocas` (`[{itemId, alimentoId}]`) é obrigatório em `TROCADO` e proibido nos outros.

## Regras aplicadas

- `id` é UUIDv7 gerado no app; se ausente, o backend gera. Repetir o mesmo `id` do mesmo paciente devolve o registro já gravado (idempotente); `id` de outro paciente dá 409.
- Vínculo precisa estar `ATIVO` (RN-VIN-05: ENCERRADO só lê, escrita dá 403 `ACESSO_NEGADO`).
- Check-in: `data` não pode ser futura nem anterior a 3 dias (fuso do usuário) — RN-ROT. A refeição precisa existir no snapshot da dieta publicada; cada troca precisa ser de item da refeição, com alimento da lista congelada no snapshot e do mesmo grupo (RN-SUB-01).
- Peso fora de 20–400 kg: 422 `VALOR_FORA_DO_INTERVALO` (RN-AVA-03).
- RN-ASS-05: assinatura vencida da nutri não bloqueia a escrita do paciente.

## POST /sync

Entidades aceitas nesta fase, só com operação `CRIAR`: `checkins`, `agua`, `pesos`. O campo `dados` tem o mesmo formato do corpo do endpoint REST correspondente (o `id` da mutação vale como `id` do registro). Cada mutação roda em transação própria; o resultado vem na mesma ordem:

- `APLICADA` com `versao` da entidade (1 para entidades só de inclusão);
- `CONFLITO` com `atual` (quando houver registro de outro dono/estado);
- `REJEITADA` com `codigo` e `regra` (ex.: `FORMATO_INVALIDO` para entidade/operação desconhecida ou dados inválidos, `ACESSO_NEGADO`, `VALOR_FORA_DO_INTERVALO`).

Reenviar o mesmo `idMutacao` devolve o resultado guardado. `idMutacao` já usado por outro usuário: `REJEITADA` / `CHAVE_IDEMPOTENCIA_REUTILIZADA`.

## GET /sync (novas coleções)

`checkins` (paciente e nutri do vínculo), `agua` (só o paciente) e `pesos` (paciente e nutris com vínculo ativo). Opcionais no JSON: ignore ausentes (`ignoreUnknownKeys`).
