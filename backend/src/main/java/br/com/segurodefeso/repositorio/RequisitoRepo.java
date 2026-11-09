package br.com.segurodefeso.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.entidade.RequisitoCliente;

public interface RequisitoRepo extends JpaRepository<RequisitoCliente, Long> {
    List<RequisitoCliente> findByClienteIdIn(Collection<Long> clienteIds);
    Optional<RequisitoCliente> findByClienteIdAndChave(Long clienteId, Requisito chave);
}
