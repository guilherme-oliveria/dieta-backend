package suporte.persistencia;

import java.util.UUID;
import com.br.dietamais.comum.api.EntidadeSincronizavel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidade_sincronizavel_teste")
public class EntidadeSincronizavelTeste extends EntidadeSincronizavel {
	@Id private UUID id;
	private String descricao;

	protected EntidadeSincronizavelTeste() { }
	public EntidadeSincronizavelTeste(UUID id) { this.id = id; this.descricao = "inicial"; }
	public void alterarDescricao(String descricao) { this.descricao = descricao; }
}
