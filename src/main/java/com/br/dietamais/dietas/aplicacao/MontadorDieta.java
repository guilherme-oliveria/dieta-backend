package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.dominio.*;
import com.br.dietamais.dietas.dominio.LinhasDieta.*;
import com.br.dietamais.dietas.infra.DietaStore;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Monta a resposta da dieta com os dados atuais da lista da nutri e os totais calculados (RN-DIE-05). */
@Component
public class MontadorDieta {
	private final DietaStore dietas;private final ConsultarAlimentosParaDieta alimentos;
	public MontadorDieta(DietaStore dietas,ConsultarAlimentosParaDieta alimentos){this.dietas=dietas;this.alimentos=alimentos;}
	public List<DietaResposta> montar(List<Dieta> lista,UUID nutri){
		var refeicoes=dietas.refeicoes(lista.stream().map(Dieta::id).toList());
		var itens=dietas.itens(refeicoes.stream().map(Refeicao::id).toList());
		var base=alimentos.consultar(nutri,itens.stream().map(Item::alimentoId).collect(Collectors.toSet()));
		var itensPorRefeicao=itens.stream().collect(Collectors.groupingBy(Item::refeicaoId));
		var refeicoesPorDieta=refeicoes.stream().collect(Collectors.groupingBy(Refeicao::dietaId));
		var respostas=new ArrayList<DietaResposta>();
		for(var dieta:lista){
			var rs=new ArrayList<RefeicaoResposta>();
			for(var refeicao:refeicoesPorDieta.getOrDefault(dieta.id(),List.of())){
				var is=new ArrayList<ItemRefeicaoResposta>();
				for(var item:itensPorRefeicao.getOrDefault(refeicao.id(),List.of())){
					var a=base.get(item.alimentoId());
					is.add(new ItemRefeicaoResposta(item.id(),a.id(),a.grupoId(),a.nome(),item.porcoes(),a.medidaCaseira(),CalculadoraTotais.gramas(a,item.porcoes()),item.observacao(),CalculadoraTotais.item(a,item.porcoes())));
				}
				rs.add(new RefeicaoResposta(refeicao.id(),refeicao.nome(),refeicao.horario(),refeicao.ordem(),is,CalculadoraTotais.somar(is.stream().map(ItemRefeicaoResposta::totais).toList())));
			}
			respostas.add(new DietaResposta(dieta.id(),dieta.pacienteId(),dieta.vinculoId(),dieta.titulo(),dieta.status(),dieta.orientacao(),rs,
				CalculadoraTotais.somar(rs.stream().map(RefeicaoResposta::totais).toList()),dieta.versao(),dieta.criadoEm(),dieta.atualizadoEm()));
		}
		return respostas;
	}
}
