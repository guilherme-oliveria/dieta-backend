package com.br.dietamais.notificacoes.infra;

import com.br.dietamais.notificacoes.api.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Usado enquanto não há credencial do FCM: nada sai do servidor. */
@Component
@ConditionalOnExpression("'${dieta.push.fcm.credenciais-json:}'.isEmpty()")
public class EnviadorPushDesligado implements EnviadorPush {
	@Override public ResultadoPush enviar(MensagemPush mensagem){return ResultadoPush.FALHA_TEMPORARIA;}
}
