package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.notificacoes.infra.AvisoStore;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Central de avisos (BE-34): cada perfil do usuário vê só os seus; nunca por id vindo do corpo (isolamento). */
@Service
public class GerenciarAvisos {
	private final AvisoStore avisos;private final UsuarioAtual usuario;private final Clock relogio;
	public GerenciarAvisos(AvisoStore avisos,UsuarioAtual usuario,Clock relogio){this.avisos=avisos;this.usuario=usuario;this.relogio=relogio;}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public AvisosResposta listar(int pagina,int tamanho,boolean soNaoLidos){
		var c=dono();var perfil=c.perfil().name();
		return new AvisosResposta(avisos.listar(c.usuarioId(),perfil,soNaoLidos,pagina,tamanho),pagina,tamanho,avisos.contar(c.usuarioId(),perfil,soNaoLidos),avisos.contar(c.usuarioId(),perfil,true));
	}
	/** Idempotente: marcar de novo devolve o aviso como está. */
	@Transactional
	public AvisoResposta marcarLido(UUID id){
		var c=dono();var perfil=c.perfil().name();
		avisos.marcarLido(id,c.usuarioId(),perfil,relogio.instant());
		var a=avisos.doUsuario(id,c.usuarioId(),perfil);if(a==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return a;
	}
	@Transactional
	public void marcarTodosLidos(){var c=dono();avisos.marcarTodosLidos(c.usuarioId(),c.perfil().name(),relogio.instant());}
	private ContextoUsuario dono(){
		var c=usuario.obter();
		if(c.perfil()!=Perfil.NUTRICIONISTA&&c.perfil()!=Perfil.PACIENTE)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		return c;
	}
}
