package com.br.dietamais.referencias.api;

import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Tabelas de referência que o app da nutri usa nas telas de necessidades (BE-43b): MET das atividades e DRI por perfil. Só leitura. */
@RestController
@RequestMapping("/api/v1/referencias")
@Tag(name="referencias")
@SecurityRequirement(name="bearerAuth")
@RotaNutri(exclusiva=false)
public class ReferenciasController {
	private final ConsultaAtividadesMet atividades;private final ConsultaDri dri;private final ConsultaTaco taco;private final ConsultaOms oms;private final ConsultaExames exames;
	public ReferenciasController(ConsultaAtividadesMet atividades,ConsultaDri dri,ConsultaTaco taco,ConsultaOms oms,ConsultaExames exames){this.atividades=atividades;this.dri=dri;this.taco=taco;this.oms=oms;this.exames=exames;}

	public record AtividadesMetResposta(List<AtividadeMet> itens){}
	public record TacoResposta(String fonte,List<String> categorias,List<AlimentoTaco> itens){}
	public record CurvaOmsResposta(String fonte,List<String> percentisRotulos,List<String> escoresZRotulos,CurvaOms curva){}
	public record ExamesResposta(String faixaEtaria,List<ExameCatalogo> itens){}
	public record MetasDriResposta(String sexo,int idadeMeses,boolean gestante,boolean lactante,List<ValorDri> itens){}

	@GetMapping("/atividades-met")
	@Operation(operationId="listarAtividadesMet",summary="Listar atividades físicas com MET",description="Tabela da aba MET da planilha; o `codigo` vai em `energia.rotinaMet.atividades`. BE-43b.")
	@ApiResponse(responseCode="200",description="Atividades",content=@Content(mediaType="application/json",schema=@Schema(implementation=AtividadesMetResposta.class)))
	public ResponseEntity<AtividadesMetResposta> listarAtividadesMet(){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new AtividadesMetResposta(atividades.listar()));
	}

	@GetMapping("/dri")
	@Operation(operationId="consultarMetasDri",summary="Consultar as ingestões de referência (DRI) de um perfil",
		description="Fibras, água, minerais, vitaminas e proteína (RDA) por sexo, idade em meses, gestação e lactação (lactante tem prioridade). Nutriente sem referência para a idade não aparece. BE-43b.")
	@ApiResponse(responseCode="200",description="Metas do perfil",content=@Content(mediaType="application/json",schema=@Schema(implementation=MetasDriResposta.class)))
	public ResponseEntity<MetasDriResposta> consultarMetasDri(@RequestParam String sexo,@RequestParam int idadeMeses,@RequestParam(defaultValue="false") boolean gestante,@RequestParam(defaultValue="false") boolean lactante){
		if(!sexo.equals("masculino")&&!sexo.equals("feminino")||idadeMeses<0||idadeMeses>1440)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new MetasDriResposta(sexo,idadeMeses,gestante&&!lactante,lactante,dri.metas(sexo,idadeMeses,gestante,lactante)));
	}

	@GetMapping("/taco")
	@Operation(operationId="buscarAlimentosTaco",summary="Buscar alimentos na TACO",description="Composição por 100 g de parte comestível (TACO, NEPA-UNICAMP, 4ª edição, 2011). `busca` são palavras do nome (todas devem aparecer, sem acento nem maiúsculas); `categoria` é exata; até `limite` itens (padrão 50, máximo 200). Nutriente ausente em `nutrientes` = não analisado; `tracos` lista os que a fonte marca como traço. A fonte deve ser citada na tela e no PDF. BE-47a.")
	@ApiResponse(responseCode="200",description="Alimentos",content=@Content(mediaType="application/json",schema=@Schema(implementation=TacoResposta.class)))
	public ResponseEntity<TacoResposta> buscarAlimentosTaco(@RequestParam(required=false) String busca,@RequestParam(required=false) String categoria,@RequestParam(defaultValue="50") int limite){
		if(limite<1||limite>200||(busca!=null&&busca.length()>100))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new TacoResposta(AlimentoTaco.FONTE,taco.categorias(),taco.buscar(busca,categoria,limite)));
	}

	@GetMapping("/taco/{numero}")
	@Operation(operationId="consultarAlimentoTaco",summary="Consultar um alimento da TACO pelo número",description="Número do alimento na tabela (1 a 597). BE-47a.")
	@ApiResponse(responseCode="200",description="Alimento",content=@Content(mediaType="application/json",schema=@Schema(implementation=AlimentoTaco.class)))
	@ApiResponse(responseCode="404",description="Número inexistente",content=@Content(mediaType="application/problem+json"))
	public ResponseEntity<AlimentoTaco> consultarAlimentoTaco(@PathVariable int numero){
		var a=taco.porNumero(numero).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(a);
	}

	@GetMapping("/oms")
	@Operation(operationId="consultarCurvaOms",summary="Consultar a curva de crescimento da OMS",description="Cortes de percentil (0,1 a 99) e de escore Z (-3 a +3) de peso, estatura ou IMC por idade, como nas abas P.I., E.I. e I.M.C. da planilha. `indicador`: PESO_IDADE, ESTATURA_IDADE ou IMC_IDADE; `sexo`: masculino ou feminino. Peso e estatura vão até 119 meses; o IMC também de 10 a 17 anos (linha do ano completo). Fora da faixa: 404. BE-54.")
	@ApiResponse(responseCode="200",description="Curva",content=@Content(mediaType="application/json",schema=@Schema(implementation=CurvaOmsResposta.class)))
	@ApiResponse(responseCode="404",description="Sem curva para a idade",content=@Content(mediaType="application/problem+json"))
	public ResponseEntity<CurvaOmsResposta> consultarCurvaOms(@RequestParam String indicador,@RequestParam String sexo,@RequestParam int idadeMeses){
		if(!CurvaOms.INDICADORES.contains(indicador)||!sexo.equals("masculino")&&!sexo.equals("feminino")||idadeMeses<0||idadeMeses>1440)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		var c=oms.curva(indicador,sexo,idadeMeses).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CurvaOmsResposta("OMS 2006/2007 (planilhas da Nut. Luciana)",CurvaOms.PERCENTIS,CurvaOms.ESCORES_Z,c));
	}

	@GetMapping("/exames")
	@Operation(operationId="listarExames",summary="Listar o catálogo de exames de uma faixa etária",description="Exames da aba av. bioquimica da planilha, por faixa etária (CRIANCA 0 a 9, ADOLESCENTE 10 a 17, ADULTO 18 ou mais), agrupados por `secao`. `condicoes` lista as fases que a nutri escolhe quando a referência depende delas (ciclo, menopausa, IMC...). BE-56a.")
	@ApiResponse(responseCode="200",description="Catálogo",content=@Content(mediaType="application/json",schema=@Schema(implementation=ExamesResposta.class)))
	public ResponseEntity<ExamesResposta> listarExames(@RequestParam String faixaEtaria){
		if(!exames.faixasEtarias().contains(faixaEtaria))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ExamesResposta(faixaEtaria,exames.catalogo(faixaEtaria)));
	}

	@GetMapping("/exames/{codigo}/classificar")
	@Operation(operationId="classificarExame",summary="Classificar o resultado de um exame",description="Devolve o rótulo da planilha (normal, alto, limite superior...) ou, sem rótulo, o `motivo`: FALTA_SEXO, FALTA_CONDICAO, FALTA_IDADE ou FORA_DAS_FAIXAS. Exame numérico usa `valor`; qualitativo usa `texto` (ausente, não reagente...). `idadeAnos` é a idade na data do exame. BE-56a.")
	@ApiResponse(responseCode="200",description="Classificação",content=@Content(mediaType="application/json",schema=@Schema(implementation=ClassificacaoExame.class)))
	@ApiResponse(responseCode="404",description="Exame inexistente na faixa",content=@Content(mediaType="application/problem+json"))
	public ResponseEntity<ClassificacaoExame> classificarExame(@PathVariable String codigo,@RequestParam String faixaEtaria,@RequestParam(required=false) java.math.BigDecimal valor,@RequestParam(required=false) String texto,
			@RequestParam(required=false) String sexo,@RequestParam(required=false) Double idadeAnos,@RequestParam(required=false) String condicao){
		if(sexo!=null&&!sexo.equals("masculino")&&!sexo.equals("feminino")||idadeAnos!=null&&(idadeAnos<0||idadeAnos>130)||valor==null&&texto==null||valor!=null&&texto!=null)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		var r=exames.classificar(faixaEtaria,codigo,valor,texto,sexo,idadeAnos,condicao).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(r);
	}
}
