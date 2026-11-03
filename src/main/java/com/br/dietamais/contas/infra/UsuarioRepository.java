package com.br.dietamais.contas.infra;

import java.time.Instant;
import java.util.UUID;
import com.br.dietamais.contas.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
	// RN-CON-01: todos os índices únicos arbitram a concorrência sem erro SQL com e-mail no log.
	@Modifying
	@Query(value = """
			insert into usuario (id, email, senha_hash, nome, perfil, ativo, criado_em, atualizado_em,
			fuso, termos_aceitos_em, versao_termos)
			values (:id, :email, :hash, :nome, 'NUTRICIONISTA', true, :agora, :agora, :fuso, :agora, :versao)
			on conflict do nothing
			""", nativeQuery = true)
	int inserirSeEmailDisponivel(@Param("id") UUID id, @Param("email") String email, @Param("hash") String hash,
			@Param("nome") String nome, @Param("agora") Instant agora, @Param("fuso") String fuso, @Param("versao") String versao);
}
