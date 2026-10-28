package com.br.dietamais.comum.api;

import java.util.UUID;

/**
 * Publicado, dentro da mesma transação, quando o paciente exclui a conta (RN-LGPD-03). Cada módulo apaga ou anonimiza o que é seu.
 * {@code contaInteira} é falso quando o mesmo login também é de nutri: só o lado paciente sai.
 */
public record ContaPacienteExcluida(UUID usuarioId,UUID pacienteId,boolean contaInteira) { }
