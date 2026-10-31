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
	private final ConsultaAtividadesMet atividades;private final ConsultaDri dri;private final ConsultaTaco taco;
	public ReferenciasController(ConsultaAtividadesMet atividades,ConsultaDri dri,ConsultaTaco taco){this.atividades=atividades;this.dri=dri;this.taco=taco;}

	public record AtividadesMetResposta(List<AtividadeMet> itens){}
	public record TacoResposta(String fonte,List<String> categorias,List<AlimentoTaco> itens){}
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
	@Operation(operationId="buscarAlimentosTaco",summary="Buscar alimentos na TACO",description="Composição por 100 g de parte comestível (TACO, NEPA-UNICAMP, 4ª edição, 2011). `busca` é um trecho do nome (sem acento); `categoria` é exata; até `limite` itens (padrão 50, máximo 200). Nutriente ausente em `nutrientes` = não analisado; `tracos` lista os que a fonte marca como traço. A fonte deve ser citada na tela e no PDF. BE-47a.")
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
}
