package com.br.dietamais.dietas.api;

import java.time.Instant;
import java.util.UUID;

/** Evento de aplicação (RN-DIE-08): consumido depois por push e feed .ics. Sem dado clínico. */
public record DietaPublicada(UUID snapshotId,UUID dietaId,UUID vinculoId,UUID pacienteId,UUID nutricionistaId,Instant publicadoEm) {}
