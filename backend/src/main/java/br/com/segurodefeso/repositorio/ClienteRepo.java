package br.com.segurodefeso.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.entidade.Cliente;

public interface ClienteRepo extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    Optional<Cliente> findByCpf(String cpf);
    List<Cliente> findByAtivoTrueOrderByNomeAsc();
    List<Cliente> findAllByOrderByNomeAsc();
    long countByAtivoTrueAndCadastroConcluidoFalse();
    long countByAtivoTrueAndEntrevista(Entrevista entrevista);

    /** Só os CPFs já cadastrados (a importação não precisa carregar os clientes inteiros). */
    @org.springframework.data.jpa.repository.Query("select c.cpf from Cliente c")
    List<String> todosOsCpfs();
}
