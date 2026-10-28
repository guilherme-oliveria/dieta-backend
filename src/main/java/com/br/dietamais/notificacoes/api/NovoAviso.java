package com.br.dietamais.notificacoes.api;

import com.br.dietamais.comum.api.Perfil;
import java.util.UUID;

/**
 * @param destinatarioId id de {@code paciente} (ficha global) ou de {@code nutricionista}, conforme o perfil
 * @param rota destino da navegação no app, sem dado clínico (ex.: {@code /dieta})
 * @param chaveDedup quando não nula, o mesmo aviso não é criado duas vezes para o usuário
 */
public record NovoAviso(Perfil perfil,UUID destinatarioId,TipoAviso tipo,String rota,String chaveDedup) {}
