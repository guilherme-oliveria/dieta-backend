package com.br.dietamais.pacientes.dominio;

import com.br.dietamais.comum.api.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.HexFormat;

public class CodigoConvite {
	private static final String ALFABETO="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private final SecureRandom aleatorio=new SecureRandom();
	public String novo(){var codigo=new StringBuilder(8);for(int i=0;i<8;i++)codigo.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));return codigo.toString();}
	public static String hash(String codigo) {
		if(codigo==null||!codigo.matches("[A-HJ-NP-Z2-9]{8}"))throw invalido();
		try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8)));}
		catch(NoSuchAlgorithmException erro){throw new IllegalStateException("SHA-256 indisponível",erro);}
	}
	public static void validar(boolean usado,boolean invalidado,Instant fim,Instant agora) {
		if(usado||invalidado)throw invalido();
		if(!agora.isBefore(fim))throw new ExcecaoNegocio(CodigoErro.CONVITE_EXPIRADO,"RN-VIN-02","O convite expirou.",java.util.List.of(),null);
	}
	public static ExcecaoNegocio invalido(){return new ExcecaoNegocio(CodigoErro.CONVITE_INVALIDO,"RN-VIN-02","Convite inválido.",java.util.List.of(),null);}
}
