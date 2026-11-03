package com.br.dietamais.avaliacoes.api;

import jakarta.validation.Valid;
import java.math.BigDecimal;

/**
 * Escolhas da nutri para o gasto energético e as metas (BE-43), todas opcionais: sem {@code metodo} vale FAO 2004; sem protocolos valem
 * faixa DRI de carboidrato e proteína e mínimo de 20% de lipídio. {@code lactacao} preenchido = lactante. O nível de atividade, o objetivo
 * e os kg por mês vêm de {@code complementares}. {@code rotinaMet} só é lida quando o método é MET.
 */
public record EnergiaAvaliacao(MetodoGet metodo,Boolean gestante,PeriodoLactacao lactacao,ProtocoloCarboidrato carboidrato,ProtocoloProteina proteina,
		ProtocoloLipidio lipidio,Boolean dislipidemico,BigDecimal colesterolTotal,@Valid RotinaMet rotinaMet) {
}
