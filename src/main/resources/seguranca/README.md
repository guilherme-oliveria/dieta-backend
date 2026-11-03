# Lista local de senhas comuns

`senhas-comuns-sha256.txt` contém somente hashes SHA-256 das entradas com pelo menos 10 caracteres da lista pública [SecLists 100k-most-used-passwords-NCSC.txt](https://github.com/danielmiessler/SecLists/blob/1a7bb9127eca9e6ff2fc0301c597fe6e16a0cb56/Passwords/Common-Credentials/100k-most-used-passwords-NCSC.txt), revisão `1a7bb9127eca9e6ff2fc0301c597fe6e16a0cb56`. As entradas foram normalizadas com remoção de espaços nas bordas e conversão para minúsculas, filtradas e deduplicadas: 9.126 hashes. A licença MIT da fonte está em `LICENSE-SecLists.txt`.

Essa lista é um bloqueio local de senhas comuns, não uma consulta exaustiva a vazamentos. Pode ser substituída pela propriedade `dieta.contas.senhas-bloqueadas` com um recurso de hashes no mesmo formato. O arquivo precisa conter hashes hexadecimais de 64 caracteres, um por linha; linhas iniciadas por `#` são comentários.

Nenhuma senha enviada pelo usuário é consultada em serviço externo. O SHA-256 serve apenas à consulta desta lista pública; credenciais da aplicação são armazenadas com Argon2id e salt aleatório, preservando exatamente a senha informada.
