package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.*;
import java.util.List;

public record PaginaAssinaturasAdminResposta(@NotNull List<AssinaturaAdminResposta> itens,@PositiveOrZero int pagina,@Positive int tamanho,@PositiveOrZero long total) { }
