package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.dominio.*;
import com.br.dietamais.dietas.dominio.LinhasDieta.*;
import com.br.dietamais.dietas.infra.DietaStore;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Monta a resposta da dieta com os dados atuais da lista da nutri e os totais calculados (RN-DIE-05, RN-CAL-07). */
@Component
public class MontadorDieta {
	private final DietaStore dietas;private final ConsultarAlimentosParaDieta alimentos;private final ConsultaPorcoes porcoes;private final JsonMapper mapper;
	public MontadorDieta(DietaStore dietas,ConsultarAlimentosParaDieta alimentos,ConsultaPorcoes porcoes,JsonMapper mapper){this.dietas=dietas;this.alimentos=alimentos;this.porcoes=porcoes;this.mapper=mapper;}
	public List<DietaResposta> montar(List<Dieta> lista,UUID nutri){
		var refeicoes=dietas.refeicoes(lista.stream().map(Dieta::id).toList());
		var itens=dietas.itens(refeicoes.stream().map(Refeicao::id).toList());
		var base=alimentos.consultar(nutri,itens.stream().map(Item::alimentoId).filter(Objects::nonNull).collect(Collectors.toSet()));
		var grupos=new HashMap<UUID,PorcoesGrupos.Grupo>();
		if(itens.stream().anyMatch(i->i.grupoId()!=null))porcoes.porcoes().grupos().forEach(g->grupos.put(g.grupoId(),g));
		var itensPorRefeicao=itens.stream().collect(Collectors.groupingBy(Item::refeicaoId));
		var refeicoesPorDieta=refeicoes.stream().collect(Collectors.groupingBy(Refeicao::dietaId));
		var respostas=new ArrayList<DietaResposta>();
		for(var dieta:lista){
			var escolhidas=lerPorcoes(dieta.porcoesJson());var metas=dieta.metasJson()==null?null:mapper.readValue(dieta.metasJson(),MetasDieta.class);
			var opcaoPorGrupo=new HashMap<UUID,Integer>();if(escolhidas!=null)escolhidas.forEach(p->opcaoPorGrupo.put(p.grupoId(),p.opcao()));
			var rs=new ArrayList<RefeicaoResposta>();var exatas=new HashMap<UUID,CalculadoraTotais.Parcela>();
			for(var refeicao:refeicoesPorDieta.getOrDefault(dieta.id(),List.of())){
				var is=new ArrayList<ItemRefeicaoResposta>();var exatos=new ArrayList<CalculadoraTotais.Parcela>();
				for(var item:itensPorRefeicao.getOrDefault(refeicao.id(),List.of())){
					var par=item(item,base,grupos,opcaoPorGrupo);is.add(par.item());exatos.add(par.exato());
				}
				var exata=CalculadoraTotais.somarExato(exatos);exatas.put(refeicao.id(),exata);
				rs.add(new RefeicaoResposta(refeicao.id(),refeicao.nome(),refeicao.horario(),refeicao.ordem(),is,exata.totais(),"ALTERNATIVA".equals(refeicao.tipo())?refeicao.tipo():null,refeicao.alternativaDe(),null));
			}
			// RN-DIE-11: a alternativa mostra a diferença para a principal e fica fora do total do dia.
			var totaisPorRefeicao=rs.stream().collect(Collectors.toMap(RefeicaoResposta::id,RefeicaoResposta::totais));
			for(int i=0;i<rs.size();i++){
				var r=rs.get(i);
				if(r.alternativa()&&totaisPorRefeicao.containsKey(r.alternativaDe()))
					rs.set(i,new RefeicaoResposta(r.id(),r.nome(),r.horario(),r.ordem(),r.itens(),r.totais(),r.tipo(),r.alternativaDe(),CalculadoraTotais.diferenca(r.totais(),totaisPorRefeicao.get(r.alternativaDe()))));
			}
			var dia=CalculadoraTotais.somarExato(rs.stream().filter(r->!r.alternativa()).map(r->exatas.get(r.id())).toList()).totais();
			if(metas!=null)dia=dia.comSaldo(CalculadoraTotais.saldo(metas,dia));
			respostas.add(new DietaResposta(dieta.id(),dieta.pacienteId(),dieta.vinculoId(),dieta.titulo(),dieta.status(),dieta.orientacao(),rs,dia,dieta.versao(),dieta.criadoEm(),dieta.atualizadoEm(),
				escolhidas,metas,dieta.pesoReferenciaKg(),dieta.rodape()));
		}
		return respostas;
	}
	private List<PorcaoGrupo> lerPorcoes(String json){return json==null?null:mapper.readValue(json,new TypeReference<List<PorcaoGrupo>>(){});}
	private record ItemMontado(ItemRefeicaoResposta item,CalculadoraTotais.Parcela exato) {}
	private ItemMontado item(Item item,Map<UUID,AlimentoParaDieta> base,Map<UUID,PorcoesGrupos.Grupo> grupos,Map<UUID,Integer> opcaoPorGrupo){
		var zero=CalculadoraTotais.ZERO;
		if(item.alimentoId()!=null){
			var a=base.get(item.alimentoId());
			if(item.aVontade())return new ItemMontado(new ItemRefeicaoResposta(item.id(),a.id(),a.grupoId(),a.nome(),null,a.medidaCaseira(),null,item.observacao(),zero,a.grupoNome(),null,Boolean.TRUE,null),CalculadoraTotais.Parcela.ZERO);
			var totais=CalculadoraTotais.item(a,item.porcoes());
			return new ItemMontado(new ItemRefeicaoResposta(item.id(),a.id(),a.grupoId(),a.nome(),item.porcoes(),a.medidaCaseira(),CalculadoraTotais.gramas(a,item.porcoes()),item.observacao(),totais,a.grupoNome()),CalculadoraTotais.exata(totais));
		}
		if(item.grupoId()!=null){
			var g=grupos.get(item.grupoId());var nome=g==null?null:g.nome();
			if(item.aVontade())return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,item.grupoId(),nome,null,null,null,item.observacao(),zero,nome,null,Boolean.TRUE,null),CalculadoraTotais.Parcela.ZERO);
			var opcao=g==null?null:g.opcoes().stream().filter(o->Objects.equals(o.opcao(),opcaoPorGrupo.get(item.grupoId()))).findFirst().orElse(null);
			if(opcao==null)return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,item.grupoId(),nome,item.porcoes(),null,null,item.observacao(),zero,nome,null,null,null),CalculadoraTotais.Parcela.ZERO);
			var parcela=CalculadoraTotais.porGrupo(opcao,item.porcoes());
			BigDecimal gramas="g".equals(opcao.unidade())?opcao.quantidade().multiply(item.porcoes()).setScale(2,java.math.RoundingMode.HALF_UP):null;
			return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,item.grupoId(),nome,item.porcoes(),opcao.medidaCaseira(),gramas,item.observacao(),parcela.totais(),nome,opcao.opcao(),null,null),parcela);
		}
		return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,null,item.textoLivre(),null,null,null,item.observacao(),zero,null,null,Boolean.TRUE,item.textoLivre()),CalculadoraTotais.Parcela.ZERO);
	}
}
