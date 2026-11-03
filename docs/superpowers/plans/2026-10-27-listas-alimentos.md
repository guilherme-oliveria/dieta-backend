# BE-10 — cópias e listas de alimentos

Base develop d6536f6 (BE-09 aprovada). Uma branch/PR; não implementar dietas/snapshots BE-11/12 nem fontes BE-14.

## Contrato primeiro

- Tornar reais GET/POST/PATCH alimentos e GET/PUT lista do paciente do C2; acrescentar PATCH /grupos-alimentos/{id} para os três macros, If-Match e resposta de grupo.
- Campos novos opcionais no grupo: versao e origemMacros (CATALOGO_ESTIMADO/CATALOGO/AJUSTE_NUTRI). Legado continua válido. Após ajuste manual, macrosEstimados pode ser omitido: origem manual não prova medição laboratorial. Corrigir fixture de desativação para ativo=false e versão atualizada.
- Documento/exemplos explicam conjunto completo de campos editáveis, UUIDv7/idempotência na criação, busca literal/paginação, referências próprias e 404, conflito com atual somente autorizado.

## Persistência e regras

- Migração 012: grupo_alimento_nutri e alimento_nutri, cópias físicas com valores/proveniência; FK composta por dono/grupo. Trigger de cadastro copia uma única vez e backfill dos existentes. Grupo mantém ID base público; alimento copiado recebe UUID próprio/catalogoId. Nunca recarregar cópia em GET nem propagar UPDATE do catálogo.
- Grupo é editável somente na cópia própria, três macros não negativos com até quatro casas; preserva kcal/porção. Origem inicial copiada integralmente; ajuste registra usuário/data e origem manual separada, versão e seq. Alimento admite criar, editar e desativar, sem DELETE.
- Lista do paciente pertence ao vínculo/ficha da nutri, não ao paciente global compartilhado. Baseline de IDs ativos é copiada ao criar vínculo, com backfill. Ajustes guardam adicionados/removidos. Alimento novo pode ser adicionado a um paciente sem ser inserido automaticamente na baseline dos outros; alterações no alimento da nutri são lidas enquanto não houver snapshot. Inativos não são oferecidos para novas dietas.
- Versão da lista efetiva muda também quando configuração própria de alimento/grupo muda. Sequência dedicada de versões das listas evita colisão ao trocar vínculo; seq_sync permanece revisão de leitura futura, não If-Match. Escritas serializadas por nutri com o mesmo advisory lock já usado em pacientes, depois locks de ficha/ajuste. GET usa snapshot REPEATABLE_READ.
- Módulo pacientes expõe porta API mínima para resolver a ficha própria atual com status/ID de vínculo. Outra nutri 404; ENCERRADO permite leitura e impede alterações. Auditoria clínica da lista contém somente identificadores.

## Testes e entrega

1. TDD de validação/conjuntos/versão, serviços/perfis/guarda/conflitos e binding JDBC quando pertinente.
2. Contratos/fixtures/OpenAPI/HTTP reais sem banco e arquitetura. Integrações Postgres escritas: cópia/cadastro/backfill, independência do base, isolamento entre nutris e pacientes, macros/origem/If-Match, desativação, idempotência, concorrência.
3. Build completo e verificação disponível, limitações Docker/CI quota explícitas, PR develop e QA: PRONTO. Somente QA aprova/mergeia; próxima tarefa após gate.

Verificação do teste legado BE-33: contrato-fase-1.md (seção de ficha) exige 400 para ausência/formato inválido de If-Match, sem especificar OBRIGATORIO para esse header; integracao.md e contrato-fase-2.md exigem a versão. O mapeamento comum vigente é FORMATO_INVALIDO para ServletRequestBindingException. Ajustada somente a asserção equivocada de PerfilNutriApiTest, sem mudar produção. ContratoIfMatchHttpTest sobe Tomcat/HttpClient reais sem banco: falta do header retorna 400 e não chama escrita; header válido permite 200/ETag. Decoder e persistência são controlados; não é prova de autorização por banco ou concorrência Postgres.
