package br.com.segurodefeso.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Dae;

public interface DaeRepo extends JpaRepository<Dae, Long> {
    List<Dae> findByClienteIdIn(Collection<Long> clienteIds);
    Optional<Dae> findByClienteIdAndCompetencia(Long clienteId, String competencia);
}
