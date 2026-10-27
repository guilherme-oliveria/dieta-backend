package com.br.dietamais.pacientes.api;

import java.util.UUID;

public interface ConsultarFichaParaLista { FichaParaLista consultar(UUID paciente,boolean bloquear); }
