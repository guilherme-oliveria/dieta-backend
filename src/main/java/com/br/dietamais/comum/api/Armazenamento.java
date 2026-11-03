package com.br.dietamais.comum.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Armazenamento de arquivos (PDF gerado, logo da nutri). Em dev e no beta grava em disco; em produção usa S3 ({@code MinIO} em dev).
 * O arquivo nunca é público: o acesso é por URL assinada com validade curta.
 */
public interface Armazenamento {
	record Acesso(String url,Instant expiraEm) { }
	void guardar(String chave,byte[] conteudo,String tipo);
	Optional<byte[]> ler(String chave);
	void apagar(String chave);
	/** Apaga o que está sob o prefixo e é mais velho que {@code idade}; devolve quantos. O S3 usa a regra de ciclo de vida do bucket e devolve 0. */
	int apagarAntigos(String prefixo,Duration idade);
	Acesso urlAssinada(String chave,Duration validade,String nomeArquivo,String tipo);
}
