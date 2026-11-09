package br.com.segurodefeso.entidade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "config")
public class ConfigItem {

    @Id
    private String chave;
    private String valor;

    public ConfigItem() {
    }

    public ConfigItem(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }

    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
