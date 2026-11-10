package br.com.segurodefeso.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Auxiliar;

public interface AuxiliarRepo extends JpaRepository<Auxiliar, Long> {

    List<Auxiliar> findAllByOrderByNomeAsc();
}
