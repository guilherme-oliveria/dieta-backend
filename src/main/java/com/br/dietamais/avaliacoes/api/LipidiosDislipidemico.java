package com.br.dietamais.avaliacoes.api;

/** Frações do lipídio total para dislipidêmico (SBC 2007): saturado 7%, mono 20%, poli 10% e trans 1% do VET. */
public record LipidiosDislipidemico(MetaMacro total,MetaMacro saturado,MetaMacro monoinsaturado,MetaMacro poliinsaturado,MetaMacro trans) {
}
