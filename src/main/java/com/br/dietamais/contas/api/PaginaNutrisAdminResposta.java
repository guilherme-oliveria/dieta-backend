package com.br.dietamais.contas.api;

import jakarta.validation.constraints.*;
import java.util.List;

public record PaginaNutrisAdminResposta(@NotNull List<NutriAdminResposta> itens,@PositiveOrZero int pagina,@Positive int tamanho,@PositiveOrZero long total) { }
