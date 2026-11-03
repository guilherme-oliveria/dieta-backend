package com.br.dietamais.assinaturas.infra;

import java.util.UUID;
import com.br.dietamais.assinaturas.dominio.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {
}
