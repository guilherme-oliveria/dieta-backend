# Fonte e publicação dos termos de uso

`terms-of-use.md` é a fonte única. O Gradle a copia para `legal/` no artefato; não mantenha outra cópia em `src/main/resources`. `GET /legal/terms-of-use`, sem Bearer, retorna o conteúdo Markdown e os metadados para os clientes.

O estado inicial é **1.0.0 / RASCUNHO**, com conteúdo final e aprovação pendentes. Nenhuma flag de ambiente ou campo enviado pelo cliente libera o cadastro.

Os metadados são quatro linhas simples, sem aspas: `versao`, `status`, `aprovadoEm`, `publicadoEm`. Datas, quando autorizadas e efetivas, usam UTC ISO-8601. Estados aceitos: RASCUNHO, APROVADO e PUBLICADO. Cadastro só é habilitado em PUBLICADO, com aprovação e publicação existentes, efetivas pelo Clock e em ordem cronológica.

Antes da liberação, o responsável deve concluir e revisar o conteúdo, obter aprovação expressa do Guilherme e autorizar a publicação inicial. Alterações de conteúdo/metadados entram por PR; o artefato precisa ser reconstruído/publicado para refletir a fonte. A revisão de código ou o merge desta implementação não aprovam nem publicam os termos jurídicos.

O backend valida a versão aceita antes de hash/escrita e antes do cache idempotente. O registro fica na própria conta (`usuario.id`, `versao_termos`, `termos_aceitos_em`), na transação de criação. Contas antigas não recebem aceite presumido. Consentimento clínico tem sua própria versão/aceite por vínculo e não é substituído por este documento.
