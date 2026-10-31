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
	private final ConsultaAtividadesMet atividades;private final ConsultaDri dri;
	public ReferenciasController(ConsultaAtividadesMet atividades,ConsultaDri dri){this.atividades=atividades;this.dri=dri;}

	public record AtividadesMetResposta(List<AtividadeMet> itens){}
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
}
