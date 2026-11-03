package com.br.dietamais.pacientes.api;

import java.util.Optional;
import java.util.UUID;

/** Nome do paciente e nutri do vínculo, para o cabeçalho dos PDFs (BE-28). O chamador já autorizou o acesso ao vínculo. */
public interface ConsultaCabecalhoRelatorio {
	Optional<Cabecalho> doVinculo(UUID vinculoId);
	record Cabecalho(String paciente,UUID nutricionistaId) { }
}
