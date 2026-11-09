package br.com.segurodefeso.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.segurodefeso.dominio.*;
import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cpf;
    private String nit;
    private LocalDate nascimento;
    private String telefone;
    private String endereco;
    private String municipio;
    private String colonia;
    private String rgpNumero;
    private String rgpDesde;
    private LocalDate rgpValidade;
    private String especie;
    private LocalDate procuracaoValidade;
    private String govNivel;
    private BigDecimal honorariosPct;
    @Enumerated(EnumType.STRING)
    private Etapa etapa;
    private String protocolo;
    private String situacao;
    private String exigenciaTexto;
    private LocalDate prazoData;
    private String indeferimentoMotivo;
    private LocalDate cienciaData;
    private int parcelasTotal;
    private int parcelasLiberadas;
    private LocalDate ultimaConferencia;
    private String observacoes;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public LocalDate getNascimento() { return nascimento; }
    public void setNascimento(LocalDate nascimento) { this.nascimento = nascimento; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getColonia() { return colonia; }
    public void setColonia(String colonia) { this.colonia = colonia; }
    public String getRgpNumero() { return rgpNumero; }
    public void setRgpNumero(String rgpNumero) { this.rgpNumero = rgpNumero; }
    public String getRgpDesde() { return rgpDesde; }
    public void setRgpDesde(String rgpDesde) { this.rgpDesde = rgpDesde; }
    public LocalDate getRgpValidade() { return rgpValidade; }
    public void setRgpValidade(LocalDate rgpValidade) { this.rgpValidade = rgpValidade; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public LocalDate getProcuracaoValidade() { return procuracaoValidade; }
    public void setProcuracaoValidade(LocalDate procuracaoValidade) { this.procuracaoValidade = procuracaoValidade; }
    public String getGovNivel() { return govNivel; }
    public void setGovNivel(String govNivel) { this.govNivel = govNivel; }
    public BigDecimal getHonorariosPct() { return honorariosPct; }
    public void setHonorariosPct(BigDecimal honorariosPct) { this.honorariosPct = honorariosPct; }
    public Etapa getEtapa() { return etapa; }
    public void setEtapa(Etapa etapa) { this.etapa = etapa; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public String getExigenciaTexto() { return exigenciaTexto; }
    public void setExigenciaTexto(String exigenciaTexto) { this.exigenciaTexto = exigenciaTexto; }
    public LocalDate getPrazoData() { return prazoData; }
    public void setPrazoData(LocalDate prazoData) { this.prazoData = prazoData; }
    public String getIndeferimentoMotivo() { return indeferimentoMotivo; }
    public void setIndeferimentoMotivo(String indeferimentoMotivo) { this.indeferimentoMotivo = indeferimentoMotivo; }
    public LocalDate getCienciaData() { return cienciaData; }
    public void setCienciaData(LocalDate cienciaData) { this.cienciaData = cienciaData; }
    public int getParcelasTotal() { return parcelasTotal; }
    public void setParcelasTotal(int parcelasTotal) { this.parcelasTotal = parcelasTotal; }
    public int getParcelasLiberadas() { return parcelasLiberadas; }
    public void setParcelasLiberadas(int parcelasLiberadas) { this.parcelasLiberadas = parcelasLiberadas; }
    public LocalDate getUltimaConferencia() { return ultimaConferencia; }
    public void setUltimaConferencia(LocalDate ultimaConferencia) { this.ultimaConferencia = ultimaConferencia; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
