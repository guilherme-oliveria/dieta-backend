package com.br.dietamais.contas.infra;

import java.util.UUID;
import com.br.dietamais.contas.dominio.Nutricionista;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutricionistaRepository extends JpaRepository<Nutricionista, UUID> {
}
