# BE-33 — perfil da nutri

Tarefa independente de BE-09 (parcial preservada em 53a2654; macros aguardam produto). Branch própria de develop c35541d e migração 011, reservando 010 para o catálogo em andamento.

1. Perfil próprio: nome de exibição efetivo (override da nutri com fallback ao nome da conta), CRN, telefone, orientação padrão, versão e data. Não alterar identidade/e-mail/senha/plano da conta global. Manter contrato BE-C2.
2. Migração 011: override opcional do nome, orientação, versão/data, telefone até 30. Atualizar somente o trigger de publicação de resumo público para incluir nome de exibição. Resolvers públicos usam mesmo nome efetivo.
3. GET/PATCH /eu/nutri reais, no-store e ETag/If-Match forte. Recurso filtrado por nid/uid/organização autenticados. Guarda de assinatura também no serviço; lock na linha do perfil antes de comparar versão. Conflito inclui somente o perfil autorizado atual. Normalizar strings opcionais vazias para nulo. Mudança de CRN invalida sua marca de verificação; sem presumir validação profissional.
4. Testes de normalização, perfil/guarda/conflito e HTTP/contrato sem banco, mais integração Postgres/isolamento/concorrência/sync público escrita e compilada (execução depende de Docker). Atualizar progresso dos 501 C2 sem alterar fixtures.
5. Build/PR develop com evidências e limitações. Ceder checkout/Gradle ao QA; só QA aprova/mergeia.
