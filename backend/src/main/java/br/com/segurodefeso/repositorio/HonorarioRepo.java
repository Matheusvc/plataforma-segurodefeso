package br.com.segurodefeso.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Honorario;

public interface HonorarioRepo extends JpaRepository<Honorario, Long> {
    List<Honorario> findByClienteIdIn(Collection<Long> clienteIds);
    List<Honorario> findByClienteIdOrderByDataRecebimentoDesc(Long clienteId);
}
