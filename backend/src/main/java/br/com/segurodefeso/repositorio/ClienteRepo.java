package br.com.segurodefeso.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Cliente;

public interface ClienteRepo extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCpf(String cpf);
    List<Cliente> findByAtivoTrueOrderByNomeAsc();
    List<Cliente> findAllByOrderByNomeAsc();
}
