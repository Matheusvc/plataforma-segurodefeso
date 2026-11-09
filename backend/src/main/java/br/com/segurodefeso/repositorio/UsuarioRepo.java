package br.com.segurodefeso.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Usuario;

public interface UsuarioRepo extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    List<Usuario> findAllByOrderByNomeAsc();
}
