package br.com.segurodefeso.entidade;


import br.com.segurodefeso.dominio.Requisito;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "requisitos")
public class RequisitoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clienteId;
    @Enumerated(EnumType.STRING)
    private Requisito chave;
    private Boolean atende;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public Requisito getChave() { return chave; }
    public void setChave(Requisito chave) { this.chave = chave; }
    public Boolean getAtende() { return atende; }
    public void setAtende(Boolean atende) { this.atende = atende; }
}
