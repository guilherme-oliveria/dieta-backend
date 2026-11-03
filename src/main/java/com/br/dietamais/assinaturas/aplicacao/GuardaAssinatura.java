package com.br.dietamais.assinaturas.aplicacao;

import com.br.dietamais.assinaturas.api.ConsultarAssinatura;
import com.br.dietamais.assinaturas.dominio.PoliticaEscritaAssinatura;
import com.br.dietamais.comum.api.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GuardaAssinatura implements GuardaEscritaNutri {
	private final UsuarioAtual usuario;
	private final ConsultarAssinatura assinaturas;
	private final PoliticaEscritaAssinatura politica;
	private final Clock relogio;
	public GuardaAssinatura(UsuarioAtual usuario,ConsultarAssinatura assinaturas,Clock relogio,@Value("${dieta.assinatura.carencia:P7D}") Duration carencia) {
		this.usuario=usuario;this.assinaturas=assinaturas;this.relogio=relogio;this.politica=new PoliticaEscritaAssinatura(carencia);
	}
	@Override public void validar() {
		var contexto=usuario.obter();
		if(contexto.perfil()!=Perfil.NUTRICIONISTA)return;
		contexto.exigirNutricionista();var assinatura=assinaturas.atual();
		if(assinatura==null||!politica.permite(assinatura.status(),assinatura.fimPeriodoEm(),relogio.instant()))
			throw new ExcecaoNegocio(CodigoErro.ASSINATURA_INATIVA,"RN-ASS-04");
	}
}
