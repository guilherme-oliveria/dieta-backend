package com.br.dietamais.pacientes.api;

import java.util.UUID;

public record FichaParaLista(UUID vinculoId,UUID pacienteId,StatusVinculo status) {}
