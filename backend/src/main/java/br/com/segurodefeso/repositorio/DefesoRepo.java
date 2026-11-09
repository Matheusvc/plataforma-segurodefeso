package br.com.segurodefeso.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.Defeso;

public interface DefesoRepo extends JpaRepository<Defeso, Long> {
    List<Defeso> findAllByOrderByInicioDesc();
}
