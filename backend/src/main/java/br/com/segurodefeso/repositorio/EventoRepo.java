package br.com.segurodefeso.repositorio;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Evento;

public interface EventoRepo extends JpaRepository<Evento, Long> {
    List<Evento> findByClienteIdOrderByMomentoDescIdDesc(Long clienteId);
    List<Evento> findAllByOrderByMomentoDescIdDesc(Pageable pageable);
}
