package com.br.dietamais.alimentos.aplicacao;

import com.br.dietamais.alimentos.api.GruposAlimentosResposta;
import com.br.dietamais.alimentos.infra.CatalogoAlimentosStore;
import com.br.dietamais.comum.api.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarGruposAlimentos {
	private final UsuarioAtual usuario;
	private final CatalogoAlimentosStore catalogo;
	public ConsultarGruposAlimentos(UsuarioAtual usuario,CatalogoAlimentosStore catalogo){this.usuario=usuario;this.catalogo=catalogo;}
	@Transactional(readOnly=true)
	public GruposAlimentosResposta executar(){
		usuario.obter().exigirNutricionista();
		return new GruposAlimentosResposta(catalogo.grupos());
	}
}
