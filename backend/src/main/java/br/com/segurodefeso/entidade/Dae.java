package br.com.segurodefeso.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.segurodefeso.dominio.*;
import jakarta.persistence.*;

@Entity
@Table(name = "dae")
public class Dae {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clienteId;
    private String competencia;
    private BigDecimal valorVenda;
    private BigDecimal valorDae;
    @Enumerated(EnumType.STRING)
    private StatusDae status;
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getCompetencia() { return competencia; }
    public void setCompetencia(String competencia) { this.competencia = competencia; }
    public BigDecimal getValorVenda() { return valorVenda; }
    public void setValorVenda(BigDecimal valorVenda) { this.valorVenda = valorVenda; }
    public BigDecimal getValorDae() { return valorDae; }
    public void setValorDae(BigDecimal valorDae) { this.valorDae = valorDae; }
    public StatusDae getStatus() { return status; }
    public void setStatus(StatusDae status) { this.status = status; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
