package br.com.segurodefeso.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.segurodefeso.dominio.*;
import jakarta.persistence.*;

@Entity
@Table(name = "honorarios")
public class Honorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clienteId;
    private BigDecimal valor;
    private LocalDate dataRecebimento;
    private String observacao;
    private Long usuarioId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataRecebimento() { return dataRecebimento; }
    public void setDataRecebimento(LocalDate dataRecebimento) { this.dataRecebimento = dataRecebimento; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
}
