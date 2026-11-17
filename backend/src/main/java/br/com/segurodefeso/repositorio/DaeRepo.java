package br.com.segurodefeso.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.segurodefeso.dominio.StatusDae;
import br.com.segurodefeso.entidade.Dae;

public interface DaeRepo extends JpaRepository<Dae, Long> {
    List<Dae> findByClienteIdIn(Collection<Long> clienteIds);
    Optional<Dae> findByClienteIdAndCompetencia(Long clienteId, String competencia);

    /** Lançamentos de uma competência feitos para clientes ativos com entrevista confirmada. */
    @Query("select count(d) from Dae d where d.competencia = :competencia and d.status = :status and d.clienteId in "
        + "(select c.id from Cliente c where c.ativo = true and c.entrevista = br.com.segurodefeso.dominio.Entrevista.CONFIRMADO)")
    long contarConfirmados(@Param("competencia") String competencia, @Param("status") StatusDae status);
}
