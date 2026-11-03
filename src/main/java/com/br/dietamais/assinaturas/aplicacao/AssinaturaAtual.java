package com.br.dietamais.assinaturas.aplicacao;

import com.br.dietamais.assinaturas.api.*;
import com.br.dietamais.comum.api.UsuarioAtual;
import com.br.dietamais.pacientes.api.OcupacaoPlano;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssinaturaAtual implements ConsultarAssinatura {
	private final JdbcTemplate jdbc;
	private final UsuarioAtual usuario;
	private final OcupacaoPlano ocupacao;
	private final Clock relogio;
	public AssinaturaAtual(JdbcTemplate jdbc,UsuarioAtual usuario,OcupacaoPlano ocupacao,Clock relogio) {
		this.jdbc=jdbc;this.usuario=usuario;this.ocupacao=ocupacao;this.relogio=relogio;
	}
	@Override @Transactional(readOnly=true)
	public AssinaturaResumoResposta atual() {
		var contexto=usuario.obter();contexto.exigirNutricionista();
		var assinaturas=jdbc.query("""
			select p.codigo,p.limite_pacientes,a.status,a.fim_periodo_em from assinatura a
			join plano p on p.id=a.plano_id where a.organizacao_id=? order by a.inicio_em desc,a.id desc limit 1
			""",(rs,n)->{
				var fim=rs.getTimestamp("fim_periodo_em");var prazo=fim==null?null:fim.toInstant();
				var status=StatusAssinatura.valueOf(rs.getString("status"));
				if(status==StatusAssinatura.TESTE&&(prazo==null||!relogio.instant().isBefore(prazo)))status=StatusAssinatura.EXPIRADA;
				return new AssinaturaResumoResposta(contexto.organizacaoId(),rs.getString("codigo"),status,rs.getInt("limite_pacientes"),0,prazo);
			},contexto.organizacaoId());
		if(assinaturas.isEmpty())return null;
		var dados=assinaturas.getFirst();
		return new AssinaturaResumoResposta(dados.organizacaoId(),dados.plano(),dados.status(),dados.limitePacientes(),ocupacao.daOrganizacaoAtual(),dados.fimPeriodoEm());
	}
}
