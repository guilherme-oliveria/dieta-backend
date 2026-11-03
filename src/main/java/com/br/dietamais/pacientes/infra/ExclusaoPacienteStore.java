package com.br.dietamais.pacientes.infra;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExclusaoPacienteStore {
	private final JdbcTemplate jdbc;
	public ExclusaoPacienteStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
	/** Perfil pessoal do paciente sem identidade; a ficha de cada nutri é prontuário dela e fica retida. */
	public void anonimizar(UUID paciente){
		jdbc.update("update paciente set nome='Titular removido',email=null,data_nascimento=null,sexo=null where id=?",paciente);
	}
	/** Encerra os vínculos abertos (a nutri passa a ver o histórico como somente leitura, RN-VIN-05). */
	public int encerrarVinculosAbertos(UUID paciente,Instant agora){
		return jdbc.update("update vinculo set status='ENCERRADO',encerrado_em=?,versao=versao+1,atualizado_em=? where paciente_id=? and status in ('CONVIDADO','ATIVO') and excluido_em is null",
			Timestamp.from(agora),Timestamp.from(agora),paciente);
	}
}
