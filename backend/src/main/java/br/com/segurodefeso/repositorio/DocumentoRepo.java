package br.com.segurodefeso.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Documento;

public interface DocumentoRepo extends JpaRepository<Documento, Long> {
    List<Documento> findByClienteIdIn(Collection<Long> clienteIds);
    List<Documento> findByClienteIdOrderByEnviadoEmDesc(Long clienteId);
}
