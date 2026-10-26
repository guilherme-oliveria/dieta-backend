package com.br.dietamais.pacientes.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.pacientes.dominio.*;
import com.br.dietamais.pacientes.infra.FichaPacienteStore;
import com.br.dietamais.pacientes.infra.ConviteStore;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GerenciarPacientes {
	private final FichaPacienteStore fichas;
	private final UsuarioAtual usuario;
	private final GuardaEscritaNutri guarda;
	private final ReservaVagaPaciente vagas;
	private final AuditoriaClinica auditoria;
	private final GeradorIds ids;
	private final Clock relogio;
	private final JsonMapper mapper;
	private final ConviteStore convites;
	public GerenciarPacientes(FichaPacienteStore fichas,UsuarioAtual usuario,GuardaEscritaNutri guarda,ReservaVagaPaciente vagas,
			AuditoriaClinica auditoria,GeradorIds ids,Clock relogio,JsonMapper mapper,ConviteStore convites) {
		this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;this.vagas=vagas;this.auditoria=auditoria;this.ids=ids;this.relogio=relogio;this.mapper=mapper;
		this.convites=convites;
	}
	@Transactional
	public PacienteResposta criar(CriarPacienteRequisicao requisicao) {
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();
		var dados=DadosFicha.de(requisicao);validarNascimento(dados);
		UUID paciente=requisicao.id()==null?ids.novo():requisicao.id();
		if(paciente.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		vagas.validar();guarda.validar();fichas.bloquearEscrita(nutri);
		validarEmail(dados.email(),nutri,null);
		fichas.criar(paciente,ids.novo(),nutri,dados,relogio.instant());
		var resposta=visivel(paciente,nutri,false);auditoria.escrita(referencia(resposta,contexto));return resposta;
	}
	@Transactional(readOnly=true)
	public PacienteResposta consultar(UUID paciente) {
		var contexto=usuario.obter();var resposta=visivel(paciente,contexto.exigirNutricionista(),false);
		auditoria.leitura(referencia(resposta,contexto));return resposta;
	}
	@Transactional
	public PacienteResposta atualizar(UUID paciente,String ifMatch,AtualizarPacienteRequisicao requisicao) {
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();
		fichas.bloquearEscrita(nutri);
		var atual=visivel(paciente,nutri,true);
		long versao=VersaoFicha.ler(ifMatch);
		if(versao!=atual.versao()) {
			auditoria.leitura(referencia(atual,contexto));
			throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A ficha foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(atual));
		}
		if(atual.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		guarda.validar();var dados=DadosFicha.de(requisicao);validarNascimento(dados);
		// O bloqueio por nutri serializa a duplicidade de e-mail sem consumir vaga ao editar.
		validarEmail(dados.email(),nutri,paciente);
		fichas.atualizar(atual.vinculoId(),nutri,dados,relogio.instant());
		if(!Objects.equals(atual.email(),dados.email()))convites.invalidar(atual.vinculoId(),relogio.instant());
		var resposta=visivel(paciente,nutri,false);auditoria.escrita(referencia(resposta,contexto));return resposta;
	}
	@Transactional(readOnly=true)
	public PaginaPacientesResposta listar(int pagina,int tamanho,String ordem,String busca,StatusVinculo status) {
		if(pagina<0||tamanho<1||tamanho>100||busca!=null&&busca.length()>150)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		String coluna=switch(ordem) {
			case "nome,asc"->"lower(f.nome) asc";case "nome,desc"->"lower(f.nome) desc";
			case "criadoEm,asc"->"f.criado_em asc";case "criadoEm,desc"->"f.criado_em desc";
			default->throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		};
		var contexto=usuario.obter();var paginaAtual=fichas.listar(contexto.exigirNutricionista(),pagina,tamanho,coluna,busca,status);
		paginaAtual.itens().forEach(item->auditoria.leitura(referencia(item,contexto)));return paginaAtual;
	}
	private PacienteResposta visivel(UUID paciente,UUID nutri,boolean bloquear) {
		var ficha=fichas.consultar(paciente,nutri,bloquear);
		if(ficha==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return ficha;
	}
	private void validarEmail(String email,UUID nutri,UUID exceto) {
		if(fichas.emailOcupado(email,nutri,exceto))throw new ExcecaoNegocio(CodigoErro.VINCULO_JA_EXISTE,"RN-VIN-07");
	}
	private void validarNascimento(DadosFicha dados) {
		if(dados.dataNascimento()!=null&&dados.dataNascimento().isAfter(LocalDate.now(relogio)))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
	}
	private ReferenciaClinica referencia(PacienteResposta ficha,ContextoUsuario contexto) {
		return new ReferenciaClinica("ficha_paciente",ficha.vinculoId(),contexto.exigirNutricionista(),ficha.id(),contexto.organizacaoId());
	}
}
