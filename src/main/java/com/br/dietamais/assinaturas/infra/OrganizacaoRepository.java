package com.br.dietamais.assinaturas.infra;

import java.util.UUID;
import com.br.dietamais.assinaturas.dominio.Organizacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoRepository extends JpaRepository<Organizacao, UUID> {
}
