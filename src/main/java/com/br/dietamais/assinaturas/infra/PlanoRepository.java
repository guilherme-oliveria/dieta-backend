package com.br.dietamais.assinaturas.infra;

import java.util.UUID;
import java.util.Optional;
import com.br.dietamais.assinaturas.dominio.Plano;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoRepository extends JpaRepository<Plano, UUID> {
	Optional<Plano> findByCodigoAndAtivoTrue(String codigo);
}
