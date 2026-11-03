package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.dominio.*;
import com.br.dietamais.dietas.dominio.LinhasDieta.*;
import com.br.dietamais.dietas.infra.DietaStore;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Monta a resposta da dieta com os dados atuais da lista da nutri e os totais calculados (RN-DIE-05, RN-CAL-07). */
@Component
public class MontadorDieta {
	private final DietaStore dietas;private final ConsultarAlimentosParaDieta alimentos;private final ConsultaPorcoes porcoes;private final ConsultaDri dri;private final ConsultarFichaParaLista fichas;private final ConsultaAvaliacaoParaDieta avaliacoes;private final Clock relogio;private final JsonMapper mapper;
	public MontadorDieta(DietaStore dietas,ConsultarAlimentosParaDieta alimentos,ConsultaPorcoes porcoes,ConsultaDri dri,ConsultarFichaParaLista fichas,ConsultaAvaliacaoParaDieta avaliacoes,Clock relogio,JsonMapper mapper){this.dietas=dietas;this.alimentos=alimentos;this.porcoes=porcoes;this.dri=dri;this.fichas=fichas;this.avaliacoes=avaliacoes;this.relogio=relogio;this.mapper=mapper;}
	public List<DietaResposta> montar(List<Dieta> lista,UUID nutri){
		var refeicoes=dietas.refeicoes(lista.stream().map(Dieta::id).toList());
		var itens=dietas.itens(refeicoes.stream().map(Refeicao::id).toList());
		var base=alimentos.consultar(nutri,itens.stream().map(Item::alimentoId).filter(Objects::nonNull).collect(Collectors.toSet()));
		var grupos=new HashMap<UUID,PorcoesGrupos.Grupo>();
		if(itens.stream().anyMatch(i->i.grupoId()!=null))porcoes.porcoes().grupos().forEach(g->grupos.put(g.grupoId(),g));
		var itensPorRefeicao=itens.stream().collect(Collectors.groupingBy(Item::refeicaoId));
		var refeicoesPorDieta=refeicoes.stream().collect(Collectors.groupingBy(Refeicao::dietaId));
		var respostas=new ArrayList<DietaResposta>();var perfis=new HashMap<UUID,Perfil>();
		for(var dieta:lista){
			var escolhidas=lerPorcoes(dieta.porcoesJson());var metas=dieta.metasJson()==null?null:mapper.readValue(dieta.metasJson(),MetasDieta.class);
			var opcaoPorGrupo=new HashMap<UUID,Integer>();if(escolhidas!=null)escolhidas.forEach(p->opcaoPorGrupo.put(p.grupoId(),p.opcao()));
			var rs=new ArrayList<RefeicaoResposta>();var exatas=new HashMap<UUID,CalculadoraTotais.Parcela>();var contribuicoes=new HashMap<UUID,List<AdequacaoDri.Contribuicao>>();
			for(var refeicao:refeicoesPorDieta.getOrDefault(dieta.id(),List.of())){
				var is=new ArrayList<ItemRefeicaoResposta>();var exatos=new ArrayList<CalculadoraTotais.Parcela>();var contribs=new ArrayList<AdequacaoDri.Contribuicao>();
				for(var item:itensPorRefeicao.getOrDefault(refeicao.id(),List.of())){
					var par=item(item,base,grupos,opcaoPorGrupo);is.add(par.item());exatos.add(par.exato());if(par.contribuicao()!=null)contribs.add(par.contribuicao());
				}
				var exata=CalculadoraTotais.somarExato(exatos);exatas.put(refeicao.id(),exata);contribuicoes.put(refeicao.id(),contribs);var micros=AdequacaoDri.somar(contribs);
				rs.add(new RefeicaoResposta(refeicao.id(),refeicao.nome(),refeicao.horario(),refeicao.ordem(),is,exata.totais().comMicronutrientes(micros.micronutrientes(),micros.incompleto()),"ALTERNATIVA".equals(refeicao.tipo())?refeicao.tipo():null,refeicao.alternativaDe(),null));
			}
			// RN-DIE-11: a alternativa mostra a diferença para a principal e fica fora do total do dia.
			var totaisPorRefeicao=rs.stream().collect(Collectors.toMap(RefeicaoResposta::id,RefeicaoResposta::totais));
			for(int i=0;i<rs.size();i++){
				var r=rs.get(i);
				if(r.alternativa()&&totaisPorRefeicao.containsKey(r.alternativaDe()))
					rs.set(i,new RefeicaoResposta(r.id(),r.nome(),r.horario(),r.ordem(),r.itens(),r.totais(),r.tipo(),r.alternativaDe(),CalculadoraTotais.diferenca(r.totais(),totaisPorRefeicao.get(r.alternativaDe()))));
			}
			var dia=CalculadoraTotais.somarExato(rs.stream().filter(r->!r.alternativa()).map(r->exatas.get(r.id())).toList()).totais();
			var principais=rs.stream().filter(r->!r.alternativa()).flatMap(r->contribuicoes.get(r.id()).stream()).toList();
			var microsDia=AdequacaoDri.somar(principais);dia=dia.comMicronutrientes(microsDia.micronutrientes(),microsDia.incompleto());
			if(metas!=null)dia=dia.comSaldo(CalculadoraTotais.saldo(metas,dia));
			var perfil=perfil(dieta.pacienteId(),nutri,perfis);
			// BE-49d: gestante e lactante vêm da avaliação mais recente (só mulher; lactante tem prioridade na tabela DRI).
			if(perfil!=null)dia=dia.comAdequacao(AdequacaoDri.adequacao(dia,dri.metas(perfil.sexo(),perfil.meses(),perfil.gestante(),perfil.lactante())));
			respostas.add(new DietaResposta(dieta.id(),dieta.pacienteId(),dieta.vinculoId(),dieta.titulo(),dieta.status(),dieta.orientacao(),rs,dia,dieta.versao(),dieta.criadoEm(),dieta.atualizadoEm(),
				escolhidas,metas,dieta.pesoReferenciaKg()!=null?dieta.pesoReferenciaKg():perfil==null?null:perfil.pesoReferenciaKg(),dieta.rodape(),
				dieta.restricoes().isEmpty()?null:dieta.restricoes().stream().map(MarcadorAlimento::valueOf).toList()));
		}
		return respostas;
	}
	private List<PorcaoGrupo> lerPorcoes(String json){return json==null?null:mapper.readValue(json,new TypeReference<List<PorcaoGrupo>>(){});}
	private record ItemMontado(ItemRefeicaoResposta item,CalculadoraTotais.Parcela exato,AdequacaoDri.Contribuicao contribuicao) {}
	private record Perfil(String sexo,int meses,boolean gestante,boolean lactante,BigDecimal pesoReferenciaKg) {}
	/** Sexo e idade da ficha para as metas DRI; sem eles (ou sem ficha) não há adequação. Gestação, lactação e o peso dos "por kg" (RN-CAL-05) vêm da avaliação mais recente, quando há. */
	private Perfil perfil(UUID paciente,UUID nutri,Map<UUID,Perfil> cache){
		if(cache.containsKey(paciente))return cache.get(paciente);
		Perfil p=null;
		try{
			var f=fichas.consultar(paciente,false);
			if(f.dataNascimento()!=null&&(f.sexo()==Sexo.MASCULINO||f.sexo()==Sexo.FEMININO)){
				var m=Period.between(f.dataNascimento(),LocalDate.now(relogio));
				int meses=m.getYears()*12+m.getMonths();
				if(meses>=0&&meses<=1440){
					var ref=avaliacoes.maisRecente(nutri,paciente).orElse(null);boolean mulher=f.sexo()==Sexo.FEMININO;
					p=new Perfil(f.sexo()==Sexo.MASCULINO?"masculino":"feminino",meses,mulher&&ref!=null&&ref.gestante(),mulher&&ref!=null&&ref.lactante(),ref==null?null:ref.pesoReferenciaKg());
				}
			}
		}catch(com.br.dietamais.comum.api.ExcecaoNegocio e){p=null;}
		cache.put(paciente,p);return p;
	}
	private ItemMontado item(Item item,Map<UUID,AlimentoParaDieta> base,Map<UUID,PorcoesGrupos.Grupo> grupos,Map<UUID,Integer> opcaoPorGrupo){
		var zero=CalculadoraTotais.ZERO;
		if(item.alimentoId()!=null){
			var a=base.get(item.alimentoId());
			if(item.aVontade())return new ItemMontado(new ItemRefeicaoResposta(item.id(),a.id(),a.grupoId(),a.nome(),null,a.medidaCaseira(),null,item.observacao(),zero,a.grupoNome(),null,Boolean.TRUE,null),CalculadoraTotais.Parcela.ZERO,null);
			var totais=CalculadoraTotais.item(a,item.porcoes());
			return new ItemMontado(new ItemRefeicaoResposta(item.id(),a.id(),a.grupoId(),a.nome(),item.porcoes(),a.medidaCaseira(),CalculadoraTotais.gramas(a,item.porcoes()),item.observacao(),totais,a.grupoNome()),CalculadoraTotais.exata(totais),new AdequacaoDri.Contribuicao(Map.of()));
		}
		if(item.grupoId()!=null){
			var g=grupos.get(item.grupoId());var nome=g==null?null:g.nome();
			if(item.aVontade())return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,item.grupoId(),nome,null,null,null,item.observacao(),zero,nome,null,Boolean.TRUE,null),CalculadoraTotais.Parcela.ZERO,null);
			var opcao=g==null?null:g.opcoes().stream().filter(o->Objects.equals(o.opcao(),opcaoPorGrupo.get(item.grupoId()))).findFirst().orElse(null);
			if(opcao==null)return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,item.grupoId(),nome,item.porcoes(),null,null,item.observacao(),zero,nome,null,null,null),CalculadoraTotais.Parcela.ZERO,null);
			var parcela=CalculadoraTotais.porGrupo(opcao,item.porcoes());
			var valores=new LinkedHashMap<String,BigDecimal>();
			opcao.nutrientes().forEach((cod,v)->{if(!AdequacaoDri.MACROS.contains(cod))valores.put(cod,v.multiply(item.porcoes()));});
			var contribuicao=new AdequacaoDri.Contribuicao(valores);var soma=AdequacaoDri.somar(List.of(contribuicao));
			BigDecimal gramas="g".equals(opcao.unidade())?opcao.quantidade().multiply(item.porcoes()).setScale(2,java.math.RoundingMode.HALF_UP):null;
			return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,item.grupoId(),nome,item.porcoes(),opcao.medidaCaseira(),gramas,item.observacao(),parcela.totais().comMicronutrientes(soma.micronutrientes(),null),nome,opcao.opcao(),null,null),parcela,contribuicao);
		}
		return new ItemMontado(new ItemRefeicaoResposta(item.id(),null,null,item.textoLivre(),null,null,null,item.observacao(),zero,null,null,Boolean.TRUE,item.textoLivre()),CalculadoraTotais.Parcela.ZERO,null);
	}
}
