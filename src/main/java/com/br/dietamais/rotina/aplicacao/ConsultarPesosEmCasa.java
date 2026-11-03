package com.br.dietamais.rotina.aplicacao;

import com.br.dietamais.rotina.api.PesosEmCasa;
import com.br.dietamais.rotina.infra.RotinaStore;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPesosEmCasa implements PesosEmCasa {
	private final RotinaStore rotina;
	public ConsultarPesosEmCasa(RotinaStore rotina){this.rotina=rotina;}
	@Override @Transactional(readOnly=true)
	public List<Ponto> doPaciente(UUID paciente,int limite){return rotina.pesosDoPaciente(paciente,limite);}
}
