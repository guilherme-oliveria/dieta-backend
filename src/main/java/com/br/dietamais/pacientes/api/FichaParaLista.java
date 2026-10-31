package com.br.dietamais.pacientes.api;

import java.time.LocalDate;
import java.util.UUID;

/** Dados mínimos da ficha para outros módulos. Sexo e nascimento são opcionais na ficha (BE-41 os usa nos cálculos da avaliação). */
public record FichaParaLista(UUID vinculoId,UUID pacienteId,StatusVinculo status,LocalDate dataNascimento,Sexo sexo) {
	public FichaParaLista(UUID vinculoId,UUID pacienteId,StatusVinculo status){this(vinculoId,pacienteId,status,null,null);}
}
