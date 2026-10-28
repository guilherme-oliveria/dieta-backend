package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.infra.PerfilNutriStore;
import org.springframework.stereotype.Service;

/** Logo da nutri para os PDFs (BE-28): PNG ou JPEG de até 512 KB, conferido pelos bytes e não pelo tipo declarado. */
@Service
public class GerenciarLogoNutri {
	static final int LIMITE_BYTES=512*1024;
	private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;private final PerfilNutriStore store;private final Armazenamento armazenamento;
	public GerenciarLogoNutri(UsuarioAtual usuario,GuardaEscritaNutri guarda,PerfilNutriStore store,Armazenamento armazenamento){
		this.usuario=usuario;this.guarda=guarda;this.store=store;this.armazenamento=armazenamento;
	}
	public void enviar(byte[] conteudo){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();
		if(conteudo==null||conteudo.length==0)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		String extensao=extensao(conteudo);
		if(extensao==null)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		if(conteudo.length>LIMITE_BYTES)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		String chave="logos/"+nutri+"."+extensao;String anterior=store.logoChave(nutri);
		armazenamento.guardar(chave,conteudo,extensao.equals("png")?"image/png":"image/jpeg");
		store.definirLogo(nutri,chave);
		if(anterior!=null&&!anterior.equals(chave))armazenamento.apagar(anterior);
	}
	public void remover(){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();
		String anterior=store.logoChave(nutri);store.definirLogo(nutri,null);
		if(anterior!=null)armazenamento.apagar(anterior);
	}
	static String extensao(byte[] b){
		if(b.length>8&&(b[0]&0xFF)==0x89&&b[1]=='P'&&b[2]=='N'&&b[3]=='G'&&b[4]==0x0D&&b[5]==0x0A&&b[6]==0x1A&&b[7]==0x0A)return "png";
		if(b.length>3&&(b[0]&0xFF)==0xFF&&(b[1]&0xFF)==0xD8&&(b[2]&0xFF)==0xFF)return "jpg";
		return null;
	}
}
