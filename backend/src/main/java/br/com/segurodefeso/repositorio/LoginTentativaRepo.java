package br.com.segurodefeso.repositorio;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.LoginTentativa;

public interface LoginTentativaRepo extends JpaRepository<LoginTentativa, Long> {
    long countByEmailAndSucessoFalseAndMomentoAfter(String email, LocalDateTime depoisDe);
}
