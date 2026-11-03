package com.br.dietamais.comum.api;

import java.util.UUID;

/** Operações de conta usadas no aceite, dentro da transação do vínculo. */
public interface ContasPaciente {
	UUID criar(String nome,String email,String senha,String fuso,Boolean aceitouTermos,String versaoTermos);
	UsuarioResposta bloquear(UUID usuario);
	SessaoResposta emitir(UUID usuario,DispositivoRequisicao dispositivo);
}
