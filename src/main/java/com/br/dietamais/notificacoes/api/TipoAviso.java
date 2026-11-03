package com.br.dietamais.notificacoes.api;

import com.br.dietamais.comum.api.Perfil;

/** Tipos de aviso da RN-NOT-01; título e corpo são genéricos (RN-NOT-02). {@code preferencia} é o nome em {@code TipoNotificacao}. */
public enum TipoAviso {
	NOVA_DIETA("NOVA_DIETA",Perfil.PACIENTE,"Nova dieta","Sua nutricionista publicou uma nova dieta."),
	QUESTIONARIO_ENVIADO("QUESTIONARIO_ENVIADO",Perfil.PACIENTE,"Novo questionário","Você tem um questionário para responder."),
	CONSULTA_AMANHA("CONSULTA_AMANHA",null,"Consulta em breve","Você tem uma consulta nas próximas 24 horas."),
	QUESTIONARIO_RESPONDIDO("QUESTIONARIO_RESPONDIDO",Perfil.NUTRICIONISTA,"Questionário respondido","Um paciente respondeu a um questionário."),
	PACIENTE_SEM_CHECKIN("PACIENTE_SEM_CHECKIN",Perfil.NUTRICIONISTA,"Paciente sem registros","Um paciente está há dias sem registrar refeições."),
	ASSINATURA("ASSINATURA",Perfil.NUTRICIONISTA,"Assinatura","Há uma novidade sobre a sua assinatura.");
	private final String preferencia;private final Perfil perfil;private final String titulo;private final String corpo;
	TipoAviso(String preferencia,Perfil perfil,String titulo,String corpo){this.preferencia=preferencia;this.perfil=perfil;this.titulo=titulo;this.corpo=corpo;}
	public String preferencia(){return preferencia;}
	/** Perfil fixo do destinatário, ou {@code null} quando vale para os dois (consulta). */
	public Perfil perfil(){return perfil;}
	public String titulo(){return titulo;}
	public String corpo(){return corpo;}
}
