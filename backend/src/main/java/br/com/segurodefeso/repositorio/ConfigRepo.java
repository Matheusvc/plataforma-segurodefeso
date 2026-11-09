package br.com.segurodefeso.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.segurodefeso.entidade.ConfigItem;

public interface ConfigRepo extends JpaRepository<ConfigItem, String> {
}
