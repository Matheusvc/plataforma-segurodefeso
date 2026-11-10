package br.com.segurodefeso.servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Analisador;
import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.ClienteDados;
import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.dominio.Cpf;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Dinheiro;
import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.dominio.StatusDae;
import br.com.segurodefeso.dominio.TipoDocumento;
import br.com.segurodefeso.entidade.Cliente;
import br.com.segurodefeso.entidade.Dae;
import br.com.segurodefeso.entidade.Documento;
import br.com.segurodefeso.entidade.Honorario;
import br.com.segurodefeso.entidade.RequisitoCliente;
import br.com.segurodefeso.repositorio.AuxiliarRepo;
import br.com.segurodefeso.repositorio.ClienteRepo;
import br.com.segurodefeso.repositorio.DaeRepo;
import br.com.segurodefeso.repositorio.DocumentoRepo;
import br.com.segurodefeso.repositorio.HonorarioRepo;
import br.com.segurodefeso.repositorio.RequisitoRepo;

/** Tudo o que altera ou analisa clientes. As regras em si ficam no pacote "dominio". */
@Service
public class ClienteService {

    /** Dados do formulário de cadastro (tudo em texto; a validação é feita aqui). */
    public record ClienteForm(String nome, String cpf, String nit, String nascimento, String telefone, String endereco,
                              String municipio, String colonia, String rgpNumero, String rgpDesde, String rgpValidade,
                              String especie, String procuracaoValidade, String govNivel, String honorariosPct,
                              String observacoes,
                              String sexo, String rg, String orgaoEmissor, String dataExpedicao, String nomePai,
                              String nomeMae, String estadoCivil, String estadoOrigem, String cidadeOrigem,
                              String tituloEleitor, String senhaGov, String inscricaoNumero, String dataAfiliacao,
                              String cep, String logradouro, String numero, String complemento, String bairro,
                              String uf, Long auxiliarId, Map<String, Boolean> respostas) {
    }

    /** Respostas da tela Atualizar status. */
    public record StatusForm(String sit, String protocolo, String exigenciaTexto, String prazo, Integer parcelasTotal,
                             Integer parcelasLiberadas, String motivo, String ciencia, String obs) {
    }

    private final ClienteRepo clientes;
    private final RequisitoRepo requisitos;
    private final DocumentoRepo documentos;
    private final DaeRepo daes;
    private final HonorarioRepo honorarios;
    private final ConfigService config;
    private final EventoService eventos;
    private final ArquivoService arquivos;
    private final AuxiliarRepo auxiliares;
    private final SegredoService segredos;
    private final Hoje hoje;

    public ClienteService(ClienteRepo clientes, RequisitoRepo requisitos, DocumentoRepo documentos, DaeRepo daes,
                          HonorarioRepo honorarios, ConfigService config, EventoService eventos, ArquivoService arquivos,
                          AuxiliarRepo auxiliares, SegredoService segredos, Hoje hoje) {
        this.auxiliares = auxiliares;
        this.segredos = segredos;
        this.clientes = clientes;
        this.requisitos = requisitos;
        this.documentos = documentos;
        this.daes = daes;
        this.honorarios = honorarios;
        this.config = config;
        this.eventos = eventos;
        this.arquivos = arquivos;
        this.hoje = hoje;
    }

    // ------------------------------------------------------------------ leitura e análise
    @Transactional(readOnly = true)
    public List<Analise> carregar(boolean incluirInativos) {
        List<Cliente> lista = incluirInativos ? clientes.findAllByOrderByNomeAsc() : clientes.findByAtivoTrueOrderByNomeAsc();
        return analisar(lista);
    }

    @Transactional(readOnly = true)
    public Analise carregar(Long id) {
        return analisar(List.of(buscar(id))).get(0);
    }

    public Cliente buscar(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado"));
    }

    private List<Analise> analisar(List<Cliente> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(Cliente::getId).toList();
        Map<Long, Map<Requisito, Boolean>> reqs = new HashMap<>();
        for (RequisitoCliente r : requisitos.findByClienteIdIn(ids)) {
            reqs.computeIfAbsent(r.getClienteId(), k -> new HashMap<>()).put(r.getChave(), r.getAtende());
        }
        Map<Long, Set<TipoDocumento>> docs = new HashMap<>();
        for (Documento d : documentos.findByClienteIdIn(ids)) {
            docs.computeIfAbsent(d.getClienteId(), k -> EnumSet.noneOf(TipoDocumento.class)).add(d.getTipo());
        }
        Map<Long, Map<String, ClienteDados.DaeInfo>> daesPorCliente = new HashMap<>();
        for (Dae d : daes.findByClienteIdIn(ids)) {
            daesPorCliente.computeIfAbsent(d.getClienteId(), k -> new HashMap<>())
                .put(d.getCompetencia(), new ClienteDados.DaeInfo(d.getStatus(), d.getValorVenda(), d.getValorDae()));
        }
        Map<Long, BigDecimal> recebido = new HashMap<>();
        for (Honorario h : honorarios.findByClienteIdIn(ids)) {
            recebido.merge(h.getClienteId(), h.getValor(), BigDecimal::add);
        }
        ConfigValores cfg = config.valores();
        LocalDate ref = hoje.data();
        List<Analise> out = new ArrayList<>();
        for (Cliente c : lista) {
            out.add(Analisador.analisar(dados(c, reqs.getOrDefault(c.getId(), Map.of()),
                docs.getOrDefault(c.getId(), Set.of()), daesPorCliente.getOrDefault(c.getId(), Map.of()),
                recebido.getOrDefault(c.getId(), BigDecimal.ZERO)), cfg, ref));
        }
        return out;
    }

    static ClienteDados dados(Cliente c, Map<Requisito, Boolean> reqs, Set<TipoDocumento> docs,
                              Map<String, ClienteDados.DaeInfo> d, BigDecimal recebido) {
        return new ClienteDados(c.getId(), c.getNome(), c.getCpf(), c.getNit(), c.getNascimento(), c.getTelefone(),
            c.getEndereco(), c.getMunicipio(), c.getColonia(), c.getRgpNumero(), c.getRgpDesde(), c.getRgpValidade(),
            c.getEspecie(), c.getProcuracaoValidade(), c.getGovNivel(), c.getHonorariosPct(), c.getEtapa(), c.getProtocolo(),
            c.getSituacao(), c.getExigenciaTexto(), c.getPrazoData(), c.getIndeferimentoMotivo(), c.getCienciaData(),
            c.getParcelasTotal(), c.getParcelasLiberadas(), c.getUltimaConferencia(), c.getObservacoes(), c.isAtivo(),
            reqs, docs, d, recebido, c.getEntrevista(),
            new ClienteDados.Pessoal(c.getSexo(), c.getRg(), c.getOrgaoEmissor(), c.getDataExpedicao(), c.getNomePai(),
                c.getNomeMae(), c.getEstadoCivil(), c.getEstadoOrigem(), c.getCidadeOrigem(), c.getTituloEleitor(),
                c.getInscricaoNumero(), c.getDataAfiliacao(), c.getCep(), c.getLogradouro(), c.getNumero(),
                c.getComplemento(), c.getBairro(), c.getUf(), c.getAuxiliarId(), c.getSenhaGovCifrada() != null));
    }

    // ------------------------------------------------------------------ cadastro
    @Transactional
    public Cliente criar(ClienteForm f, Long usuarioId) {
        Cliente c = new Cliente();
        preencher(c, f, null);
        c.setEtapa(Etapa.DOCUMENTACAO);
        c.setParcelasTotal(config.valores().parcelasPadrao());
        c.setAtivo(true);
        c.setCriadoEm(hoje.agora());
        c.setAtualizadoEm(hoje.agora());
        c.setEntrevista(Entrevista.A_CONFIRMAR);
        c = clientes.save(c);
        eventos.registrar(c.getId(), usuarioId, "cadastro", "Cliente cadastrado na plataforma");
        if (f.respostas() != null && !f.respostas().isEmpty()) {
            Map<Requisito, Boolean> r = new HashMap<>();
            f.respostas().forEach((k, v) -> r.put(Requisito.valueOf(k), v));
            salvarRequisitos(c.getId(), r, usuarioId);
        }
        return c;
    }

    @Transactional
    public void alterarEntrevista(Long id, Entrevista nova, Long usuarioId) {
        Cliente c = buscar(id);
        if (c.getEntrevista() == nova) {
            return;
        }
        c.setEntrevista(nova);
        c.setAtualizadoEm(hoje.agora());
        clientes.save(c);
        eventos.registrar(id, usuarioId, "entrevista", "Entrevista: " + nova.getNome());
    }

    /** Devolve a senha gov em texto. Cada leitura fica registrada na linha do tempo. */
    @Transactional
    public String revelarSenhaGov(Long id, Long usuarioId) {
        Cliente c = buscar(id);
        if (c.getSenhaGovCifrada() == null) {
            throw new RegraException("Nenhuma senha gov cadastrada para este cliente.");
        }
        eventos.registrar(id, usuarioId, "acesso_senha_gov", "Senha gov consultada");
        return segredos.decifrar(c.getSenhaGovCifrada());
    }

    @Transactional
    public Cliente atualizar(Long id, ClienteForm f, Long usuarioId) {
        Cliente c = buscar(id);
        preencher(c, f, id);
        c.setAtualizadoEm(hoje.agora());
        clientes.save(c);
        eventos.registrar(id, usuarioId, "cadastro", "Dados do cadastro alterados");
        atualizarEtapaAutomatica(id, usuarioId);
        return c;
    }

    private void preencher(Cliente c, ClienteForm f, Long idAtual) {
        List<String> erros = new ArrayList<>();
        String nome = limpar(f.nome());
        String cpf = Cpf.digitos(f.cpf());
        if (nome == null) erros.add("Nome é obrigatório.");
        if (cpf.isEmpty()) erros.add("CPF é obrigatório.");
        else if (!Cpf.valido(cpf)) erros.add("CPF inválido: confira os números.");
        else clientes.findByCpf(cpf).filter(o -> !o.getId().equals(idAtual))
            .ifPresent(o -> erros.add("Já existe cliente com este CPF: " + o.getNome() + "."));
        if (limpar(f.telefone()) == null) erros.add("Telefone/WhatsApp é obrigatório.");
        if (limpar(f.municipio()) == null) erros.add("Município é obrigatório.");
        if (limpar(f.rgpNumero()) == null) erros.add("Nº do RGP é obrigatório.");
        LocalDate nasc = data(f.nascimento(), "Data de nascimento", erros);
        LocalDate rgpVal = data(f.rgpValidade(), "Validade do RGP", erros);
        LocalDate procVal = data(f.procuracaoValidade(), "Validade da procuração", erros);
        BigDecimal pct = null;
        if (limpar(f.honorariosPct()) != null) {
            pct = Dinheiro.ler(f.honorariosPct());
            if (pct == null || pct.signum() < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
                erros.add("Honorários: use um percentual entre 0 e 100.");
            }
        }
        String gov = limpar(f.govNivel());
        gov = gov == null ? null : gov.toLowerCase();
        if (gov != null && !Set.of("bronze", "prata", "ouro").contains(gov)) {
            gov = null;
        }
        LocalDate expedicao = data(f.dataExpedicao(), "Data da expedição", erros);
        LocalDate afiliacao = data(f.dataAfiliacao(), "Data da afiliação", erros);
        String cep = f.cep() == null ? "" : f.cep().replaceAll("\\D", "");
        if (!cep.isEmpty() && cep.length() != 8) erros.add("CEP deve ter 8 números.");
        String uf = uf(f.uf(), "Estado do endereço", erros);
        String ufOrigem = uf(f.estadoOrigem(), "Estado de origem", erros);
        if (f.auxiliarId() != null && !auxiliares.existsById(f.auxiliarId())) erros.add("Auxiliar jurídico não encontrado.");
        if (!erros.isEmpty()) {
            throw new RegraException(String.join(" ", erros));
        }
        c.setSexo(limpar(f.sexo()));
        c.setRg(limpar(f.rg()));
        c.setOrgaoEmissor(limpar(f.orgaoEmissor()));
        c.setDataExpedicao(expedicao);
        c.setNomePai(limpar(f.nomePai()));
        c.setNomeMae(limpar(f.nomeMae()));
        c.setEstadoCivil(limpar(f.estadoCivil()));
        c.setEstadoOrigem(ufOrigem);
        c.setCidadeOrigem(limpar(f.cidadeOrigem()));
        c.setTituloEleitor(limpar(f.tituloEleitor()));
        c.setInscricaoNumero(limpar(f.inscricaoNumero()));
        c.setDataAfiliacao(afiliacao);
        c.setCep(cep.isEmpty() ? null : cep);
        c.setLogradouro(limpar(f.logradouro()));
        c.setNumero(limpar(f.numero()));
        c.setComplemento(limpar(f.complemento()));
        c.setBairro(limpar(f.bairro()));
        c.setUf(uf);
        c.setAuxiliarId(f.auxiliarId());
        if (limpar(f.senhaGov()) != null) {
            c.setSenhaGovCifrada(segredos.cifrar(f.senhaGov()));
        }
        c.setNome(nome);
        c.setCpf(cpf);
        c.setNit(limpar(f.nit()));
        c.setNascimento(nasc);
        c.setTelefone(limpar(f.telefone()));
        c.setEndereco(limpar(f.endereco()));
        c.setMunicipio(limpar(f.municipio()));
        c.setColonia(limpar(f.colonia()));
        c.setRgpNumero(limpar(f.rgpNumero()));
        c.setRgpDesde(limpar(f.rgpDesde()));
        c.setRgpValidade(rgpVal);
        c.setEspecie(limpar(f.especie()));
        c.setProcuracaoValidade(procVal);
        c.setGovNivel(gov);
        c.setHonorariosPct(pct);
        c.setObservacoes(limpar(f.observacoes()));
    }

    @Transactional
    public void salvarRequisitos(Long id, Map<Requisito, Boolean> respostas, Long usuarioId) {
        buscar(id);
        for (Requisito r : Requisito.values()) {
            RequisitoCliente rc = requisitos.findByClienteIdAndChave(id, r).orElseGet(() -> {
                RequisitoCliente n = new RequisitoCliente();
                n.setClienteId(id);
                n.setChave(r);
                return n;
            });
            rc.setAtende(respostas.get(r));
            requisitos.save(rc);
        }
        eventos.registrar(id, usuarioId, "requisitos", "Requisitos da triagem atualizados");
        atualizarEtapaAutomatica(id, usuarioId);
    }

    @Transactional
    public void arquivar(Long id, Long usuarioId) {
        Cliente c = buscar(id);
        c.setAtivo(!c.isAtivo());
        c.setAtualizadoEm(hoje.agora());
        clientes.save(c);
        eventos.registrar(id, usuarioId, "cadastro", c.isAtivo() ? "Cliente reativado" : "Cliente arquivado");
    }

    /** Documentação completa e requisitos ok: passa sozinho para "A requerer". */
    @Transactional
    public void atualizarEtapaAutomatica(Long id, Long usuarioId) {
        clientes.flush();
        Analise a = carregar(id);
        if (a.cliente().etapa() == Etapa.DOCUMENTACAO && a.apto()) {
            Cliente c = buscar(id);
            c.setEtapa(Etapa.A_REQUERER);
            c.setAtualizadoEm(hoje.agora());
            clientes.save(c);
            eventos.registrar(id, usuarioId, "etapa", "Requisitos e documentos completos: cliente pronto para requerer");
        }
    }

    // ------------------------------------------------------------------ documentos
    @Transactional
    public Documento anexar(Long id, TipoDocumento tipo, String competencia, MultipartFile arquivo, Long usuarioId) {
        buscar(id);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraException("Escolha o arquivo.");
        }
        String nome = arquivos.salvar(id, arquivo);
        Documento d = new Documento();
        d.setClienteId(id);
        d.setTipo(tipo);
        d.setCompetencia(limpar(competencia));
        String original = arquivo.getOriginalFilename() == null ? nome : arquivo.getOriginalFilename();
        d.setNomeOriginal(original.length() > 200 ? original.substring(0, 200) : original);
        d.setArquivo(nome);
        d.setTamanho(arquivo.getSize());
        d.setEnviadoPor(usuarioId);
        d.setEnviadoEm(hoje.agora());
        Documento salvo = documentos.save(d);
        String comp = salvo.getCompetencia();
        String desc = tipo.getNome() + " anexado";
        if (tipo == TipoDocumento.COMPROVANTE_DAE && comp != null) {
            Dae dae = daes.findByClienteIdAndCompetencia(id, comp).orElseGet(() -> {
                Dae n = new Dae();
                n.setClienteId(id);
                n.setCompetencia(comp);
                return n;
            });
            dae.setStatus(StatusDae.PAGO);
            dae.setAtualizadoEm(hoje.agora());
            daes.save(dae);
            desc += " (" + Datas.competenciaLabel(comp) + "): DAE marcado como pago";
        }
        eventos.registrar(id, usuarioId, "documento", desc);
        atualizarEtapaAutomatica(id, usuarioId);
        return salvo;
    }

    @Transactional
    public void excluirDocumento(Long docId, Long usuarioId) {
        Documento d = documentos.findById(docId).orElseThrow(() -> new NaoEncontradoException("Documento não encontrado"));
        documentos.delete(d);
        arquivos.excluir(d.getClienteId(), d.getArquivo());
        eventos.registrar(d.getClienteId(), usuarioId, "documento", d.getTipo().getNome() + " excluído (" + d.getNomeOriginal() + ")");
    }

    // ------------------------------------------------------------------ status no Emprega Brasil
    @Transactional
    public String atualizarStatus(Long id, StatusForm f, Long usuarioId) {
        Cliente c = buscar(id);
        String sit = f.sit() == null ? "" : f.sit();
        String obs = limpar(f.obs());
        String protocolo = limpar(f.protocolo()) != null ? limpar(f.protocolo()) : c.getProtocolo();
        String resumo;
        switch (sit) {
            case "analise" -> {
                c.setEtapa(Etapa.REQUERIDO);
                c.setSituacao("Em análise" + (obs != null ? ". " + obs : ""));
                c.setPrazoData(null);
                c.setExigenciaTexto(null);
                resumo = "Em análise";
            }
            case "exigencia" -> {
                String texto = limpar(f.exigenciaTexto());
                LocalDate prazo = Datas.ler(f.prazo());
                if (texto == null || prazo == null) {
                    throw new RegraException("Informe o que foi pedido na exigência e o prazo (dd/mm/aaaa).");
                }
                c.setEtapa(Etapa.EXIGENCIA);
                c.setExigenciaTexto(texto);
                c.setPrazoData(prazo);
                c.setSituacao("Exigência" + (obs != null ? ". " + obs : ""));
                resumo = "Exigência (" + texto + "), prazo " + Datas.br(prazo);
            }
            case "deferido" -> {
                int tot = f.parcelasTotal() != null && f.parcelasTotal() > 0 ? f.parcelasTotal()
                    : (c.getParcelasTotal() > 0 ? c.getParcelasTotal() : config.valores().parcelasPadrao());
                int lib = Math.max(0, Math.min(f.parcelasLiberadas() == null ? 0 : f.parcelasLiberadas(), tot));
                c.setEtapa(Etapa.DEFERIDO);
                c.setParcelasTotal(tot);
                c.setParcelasLiberadas(lib);
                c.setPrazoData(null);
                c.setExigenciaTexto(null);
                c.setSituacao("Deferido" + (obs != null ? ". " + obs : ""));
                resumo = "Deferido, " + lib + " de " + tot + " parcelas liberadas";
            }
            case "indeferido" -> {
                String motivo = limpar(f.motivo());
                if (motivo == null) {
                    throw new RegraException("Informe o motivo do indeferimento que aparece no site.");
                }
                c.setEtapa(Etapa.INDEFERIDO);
                c.setIndeferimentoMotivo(motivo);
                c.setCienciaData(Datas.ler(f.ciencia()));
                c.setPrazoData(null);
                c.setSituacao("Indeferido" + (obs != null ? ". " + obs : ""));
                resumo = "Indeferido (" + motivo + ")";
            }
            case "naoreq" -> {
                if (c.getEtapa() != Etapa.DOCUMENTACAO) {
                    c.setEtapa(Etapa.A_REQUERER);
                }
                c.setSituacao("Ainda não requerido");
                resumo = "Ainda não requerido";
            }
            case "naoachou" -> {
                c.setSituacao("Pedido não localizado no site. Confirmar com o cliente." + (obs != null ? " " + obs : ""));
                resumo = "Pedido não localizado no site";
            }
            default -> throw new RegraException("Responda primeiro: qual é a situação do pedido no site?");
        }
        c.setProtocolo(protocolo);
        c.setUltimaConferencia(hoje.data());
        c.setAtualizadoEm(hoje.agora());
        clientes.save(c);
        eventos.registrar(id, usuarioId, "status", "Status conferido no Emprega Brasil: " + resumo
            + (protocolo != null ? " (protocolo " + protocolo + ")" : ""));
        return c.getNome() + " atualizado: " + resumo + ".";
    }

    // ------------------------------------------------------------------ honorários
    @Transactional
    public void registrarHonorario(Long id, String valorTxt, String dataTxt, String observacao, Long usuarioId) {
        Analise a = carregar(id);
        BigDecimal valor = Dinheiro.ler(valorTxt);
        if (valor == null) {
            valor = a.honorariosPorParcela();
        }
        if (valor.signum() <= 0) {
            throw new RegraException("Informe um valor maior que zero.");
        }
        LocalDate data = Datas.ler(dataTxt);
        Honorario h = new Honorario();
        h.setClienteId(id);
        h.setValor(valor);
        h.setDataRecebimento(data != null ? data : hoje.data());
        h.setObservacao(limpar(observacao));
        h.setUsuarioId(usuarioId);
        honorarios.save(h);
        eventos.registrar(id, usuarioId, "honorarios", "Honorário recebido: " + Dinheiro.brl(valor) + " em " + Datas.br(h.getDataRecebimento()));
    }

    // ------------------------------------------------------------------ auxiliares
    static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String uf(String txt, String rotulo, List<String> erros) {
        String u = limpar(txt);
        if (u == null) {
            return null;
        }
        u = u.toUpperCase();
        if (!u.matches("[A-Z]{2}")) {
            erros.add(rotulo + ": use a sigla com 2 letras (ex.: MA).");
        }
        return u;
    }

    private static LocalDate data(String txt, String rotulo, List<String> erros) {
        if (limpar(txt) == null) {
            return null;
        }
        LocalDate d = Datas.ler(txt);
        if (d == null) {
            erros.add(rotulo + ": data inválida (use dd/mm/aaaa).");
        }
        return d;
    }
}
