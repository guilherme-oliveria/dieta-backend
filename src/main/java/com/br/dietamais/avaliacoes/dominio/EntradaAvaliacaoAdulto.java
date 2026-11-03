package com.br.dietamais.avaliacoes.dominio;

import com.br.dietamais.avaliacoes.api.*;
import java.math.BigDecimal;

/** Dados do cálculo antropométrico de adulto e idoso (medidas em kg, m, cm e mm). Nulo = não medido. */
public record EntradaAvaliacaoAdulto(String sexo,int idadeAnos,BigDecimal pesoKg,BigDecimal estaturaM,ProtocoloDobras protocoloDobras,
		ProtocoloMassa protocoloMassa,BigDecimal triceps,BigDecimal peitoral,BigDecimal axilarMedia,BigDecimal subescapular,
		BigDecimal abdominal,BigDecimal suprailiaca,BigDecimal coxa,BigDecimal cinturaCm,BigDecimal punhoCm,
		BigDecimal diametroPunhoCm,BigDecimal diametroJoelhoCm,BigDecimal percentualDesejado) {
}
