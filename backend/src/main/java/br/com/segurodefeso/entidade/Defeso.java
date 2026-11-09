package br.com.segurodefeso.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.segurodefeso.dominio.*;
import jakarta.persistence.*;

@Entity
@Table(name = "defesos")
public class Defeso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String especie;
    private String regiao;
    private String portaria;
    private LocalDate inicio;
    private LocalDate fim;
    private LocalDate prazoRequerimento;
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public String getRegiao() { return regiao; }
    public void setRegiao(String regiao) { this.regiao = regiao; }
    public String getPortaria() { return portaria; }
    public void setPortaria(String portaria) { this.portaria = portaria; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate inicio) { this.inicio = inicio; }
    public LocalDate getFim() { return fim; }
    public void setFim(LocalDate fim) { this.fim = fim; }
    public LocalDate getPrazoRequerimento() { return prazoRequerimento; }
    public void setPrazoRequerimento(LocalDate prazoRequerimento) { this.prazoRequerimento = prazoRequerimento; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
