package com.br.dietamais.relatorios.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.relatorios.api.ExportacaoResposta;
import com.br.dietamais.relatorios.infra.ExportacaoStore;
import com.br.dietamais.relatorios.infra.ExportacaoStore.Pedido;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pedido e consulta de exportação dos dados do paciente (RN-LGPD-02). A geração roda em segundo plano ({@link ProcessadorExportacao}). */
@Service
public class ServicoExportacao {
	static final Duration INTERVALO_MINIMO=Duration.ofHours(1);
	static final Duration VALIDADE_LINK_TELA=Duration.ofMinutes(15);
	private final ExportacaoStore store;private final UsuarioAtual usuario;private final GeradorIds ids;private final Clock relogio;private final Armazenamento armazenamento;
	public ServicoExportacao(ExportacaoStore store,UsuarioAtual usuario,GeradorIds ids,Clock relogio,Armazenamento armazenamento){
		this.store=store;this.usuario=usuario;this.ids=ids;this.relogio=relogio;this.armazenamento=armazenamento;
	}

	@Transactional
	public ExportacaoResposta pedir(){
		var contexto=usuario.obter();contexto.exigirPaciente();var u=contexto.usuarioId();var agora=relogio.instant();
		store.bloquearUsuario(u);
		var anterior=store.maisRecente(u);
		if(anterior.isPresent()){
			var a=anterior.get();
			boolean emCurso=a.status().equals("PENDENTE");
			boolean recente=!a.status().equals("FALHOU")&&Duration.between(a.criadoEm(),agora).compareTo(INTERVALO_MINIMO)<0;
			if(emCurso||recente)throw new ExcecaoNegocio(CodigoErro.EXPORTACAO_EM_ANDAMENTO,"RN-LGPD-02");
		}
		var id=ids.novo();store.inserir(id,u,agora);
		return new ExportacaoResposta(id,"PENDENTE",agora,null,null,null);
	}

	/** O pedido mais recente do titular; com {@code PRONTA} renova o link de download. */
	@Transactional(readOnly=true)
	public ExportacaoResposta ultima(){
		var contexto=usuario.obter();contexto.exigirPaciente();
		var pedido=store.maisRecente(contexto.usuarioId()).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		return resposta(pedido,relogio.instant());
	}

	ExportacaoResposta resposta(Pedido p,Instant agora){
		String url=null;
		if(p.status().equals("PRONTA")&&p.chave()!=null&&p.expiraEm()!=null&&p.expiraEm().isAfter(agora)){
			var validade=Duration.ofSeconds(Math.min(VALIDADE_LINK_TELA.toSeconds(),Duration.between(agora,p.expiraEm()).toSeconds()));
			url=armazenamento.urlAssinada(p.chave(),validade,"meus-dados-dietamais.zip","application/zip").url();
		}
		String status=p.status().equals("PRONTA")&&url==null?"EXPIRADA":p.status();
		return new ExportacaoResposta(p.id(),status,p.criadoEm(),p.prontaEm(),p.expiraEm(),url);
	}

	/** Link público do e-mail: o token aleatório vira um redirecionamento curto para o arquivo. */
	@Transactional(readOnly=true)
	public java.util.Optional<String> urlPorToken(String token){
		var agora=relogio.instant();
		return store.porHashToken(ProcessadorExportacao.hash(token)).filter(p->p.expiraEm()!=null&&p.expiraEm().isAfter(agora)&&p.chave()!=null)
			.map(p->armazenamento.urlAssinada(p.chave(),Duration.ofMinutes(5),"meus-dados-dietamais.zip","application/zip").url());
	}
}
