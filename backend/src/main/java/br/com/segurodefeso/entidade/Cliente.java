package br.com.segurodefeso.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.Etapa;
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

    private String sexo;
    private String rg;
    private String orgaoEmissor;
    private LocalDate dataExpedicao;
    private String nomePai;
    private String nomeMae;
    private String estadoCivil;
    private String estadoOrigem;
    private String cidadeOrigem;
    private String tituloEleitor;
    private String inscricaoNumero;
    private LocalDate dataAfiliacao;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String uf;
    private String senhaGovCifrada;
    private Long auxiliarId;
    @Enumerated(EnumType.STRING)
    private Entrevista entrevista = Entrevista.A_CONFIRMAR;

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
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getOrgaoEmissor() { return orgaoEmissor; }
    public void setOrgaoEmissor(String orgaoEmissor) { this.orgaoEmissor = orgaoEmissor; }
    public LocalDate getDataExpedicao() { return dataExpedicao; }
    public void setDataExpedicao(LocalDate dataExpedicao) { this.dataExpedicao = dataExpedicao; }
    public String getNomePai() { return nomePai; }
    public void setNomePai(String nomePai) { this.nomePai = nomePai; }
    public String getNomeMae() { return nomeMae; }
    public void setNomeMae(String nomeMae) { this.nomeMae = nomeMae; }
    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String estadoCivil) { this.estadoCivil = estadoCivil; }
    public String getEstadoOrigem() { return estadoOrigem; }
    public void setEstadoOrigem(String estadoOrigem) { this.estadoOrigem = estadoOrigem; }
    public String getCidadeOrigem() { return cidadeOrigem; }
    public void setCidadeOrigem(String cidadeOrigem) { this.cidadeOrigem = cidadeOrigem; }
    public String getTituloEleitor() { return tituloEleitor; }
    public void setTituloEleitor(String tituloEleitor) { this.tituloEleitor = tituloEleitor; }
    public String getInscricaoNumero() { return inscricaoNumero; }
    public void setInscricaoNumero(String inscricaoNumero) { this.inscricaoNumero = inscricaoNumero; }
    public LocalDate getDataAfiliacao() { return dataAfiliacao; }
    public void setDataAfiliacao(LocalDate dataAfiliacao) { this.dataAfiliacao = dataAfiliacao; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getSenhaGovCifrada() { return senhaGovCifrada; }
    public void setSenhaGovCifrada(String senhaGovCifrada) { this.senhaGovCifrada = senhaGovCifrada; }
    public Long getAuxiliarId() { return auxiliarId; }
    public void setAuxiliarId(Long auxiliarId) { this.auxiliarId = auxiliarId; }
    public Entrevista getEntrevista() { return entrevista; }
    public void setEntrevista(Entrevista entrevista) { this.entrevista = entrevista; }
}
