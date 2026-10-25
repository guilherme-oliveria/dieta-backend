package com.br.dietamais.assinaturas.aplicacao;

import com.br.dietamais.assinaturas.api.ConsultarAssinatura;
import com.br.dietamais.assinaturas.dominio.PoliticaLimitePlano;
import com.br.dietamais.comum.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ServicoLimitePlano implements ReservaVagaPaciente {
	private final JdbcTemplate jdbc;
	private final UsuarioAtual usuario;
	private final ConsultarAssinatura assinaturas;
	public ServicoLimitePlano(JdbcTemplate jdbc,UsuarioAtual usuario,ConsultarAssinatura assinaturas){this.jdbc=jdbc;this.usuario=usuario;this.assinaturas=assinaturas;}
	@Override @Transactional(propagation=Propagation.MANDATORY)
	public void validar() {
		var contexto=usuario.obter();contexto.exigirNutricionista();
		jdbc.queryForObject("select id from organizacao where id=? for update",java.util.UUID.class,contexto.organizacaoId());
		var plano=assinaturas.atual();
		if(plano==null)throw new ExcecaoNegocio(CodigoErro.ASSINATURA_INATIVA,"RN-ASS-04");
		PoliticaLimitePlano.validar(plano.pacientesOcupados(),plano.limitePacientes());
	}
}
