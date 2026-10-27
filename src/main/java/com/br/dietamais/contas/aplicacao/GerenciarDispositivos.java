package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.api.*;
import com.br.dietamais.contas.dominio.VersaoPerfilNutri;
import com.br.dietamais.contas.infra.DispositivoStore;
import com.br.dietamais.contas.infra.PreferenciaNotificacaoStore;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Token FCM e preferências de push (RN-NOT-03). Valem para qualquer perfil, inclusive com assinatura vencida: não são dado clínico. */
@Service
public class GerenciarDispositivos {
	private static final SilencioNotificacao SILENCIO_PADRAO=new SilencioNotificacao(true,java.time.LocalTime.of(22,0),java.time.LocalTime.of(7,0));
	private final UsuarioAtual usuario;private final DispositivoStore dispositivos;private final PreferenciaNotificacaoStore preferencias;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarDispositivos(UsuarioAtual usuario,DispositivoStore dispositivos,PreferenciaNotificacaoStore preferencias,Clock relogio,JsonMapper mapper){
		this.usuario=usuario;this.dispositivos=dispositivos;this.preferencias=preferencias;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional
	public DispositivoResposta registrar(RegistrarDispositivoRequisicao r){
		return dispositivos.registrar(usuario.obter().usuarioId(),r.token().strip(),r.plataforma(),r.nomeAparelho(),relogio.instant());
	}
	@Transactional
	public void remover(UUID id){
		if(!dispositivos.remover(usuario.obter().usuarioId(),id))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
	}
	@Transactional(readOnly=true)
	public PreferenciasNotificacaoResposta consultar(){
		var c=usuario.obter();return montar(c.perfil(),preferencias.consultar(c.usuarioId(),false));
	}
	@Transactional
	public PreferenciasNotificacaoResposta atualizar(String ifMatch,AtualizarPreferenciasRequisicao r){
		var c=usuario.obter();var perfil=c.perfil();
		long esperado=VersaoPerfilNutri.ler(ifMatch);
		var validos=tiposDe(perfil);var vistos=new HashSet<TipoNotificacao>();
		for(var t:r.tipos())if(!validos.contains(t.tipo())||!vistos.add(t.tipo()))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-NOT-03");
		if(r.silencio().inicio().equals(r.silencio().fim()))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-NOT-03");
		preferencias.garantir(c.usuarioId(),relogio.instant());
		var atual=preferencias.consultar(c.usuarioId(),true);
		if(atual.versao()!=esperado)throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","As preferências foram atualizadas. Revise os dados atuais.",List.of(),mapper.valueToTree(montar(perfil,atual)));
		// tipos do outro perfil da mesma conta ficam como estavam; os ausentes da requisição ficam ligados
		var desligados=new LinkedHashSet<TipoNotificacao>();
		atual.desligados().stream().filter(t->t.perfil()!=perfil).forEach(desligados::add);
		r.tipos().stream().filter(t->!t.ativo()).forEach(t->desligados.add(t.tipo()));
		preferencias.gravar(c.usuarioId(),List.copyOf(desligados),r.silencio(),relogio.instant());
		return montar(perfil,preferencias.consultar(c.usuarioId(),false));
	}
	private PreferenciasNotificacaoResposta montar(Perfil perfil,PreferenciaNotificacaoStore.Linha linha){
		var desligados=linha==null?List.<TipoNotificacao>of():linha.desligados();
		var tipos=tiposDe(perfil).stream().map(t->new PreferenciaTipo(t,!desligados.contains(t))).toList();
		return new PreferenciasNotificacaoResposta(tipos,linha==null?SILENCIO_PADRAO:linha.silencio(),linha==null?0L:linha.versao());
	}
	private List<TipoNotificacao> tiposDe(Perfil perfil){return Arrays.stream(TipoNotificacao.values()).filter(t->t.perfil()==perfil).toList();}
}
