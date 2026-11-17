package br.com.segurodefeso.servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import br.com.segurodefeso.dominio.Telefone;
import br.com.segurodefeso.dominio.TipoDocumento;
import static br.com.segurodefeso.dominio.Texto.limpar;
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
                              String uf, Long auxiliarId, Map<String, Boolean> respostas, Boolean emAndamento) {
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

    public record Pagina(List<Analise> itens, long total, int pagina, int tamanho) {
    }

    public record LinhaEntrevista(Long id, String nome, String municipio, String colonia, String etapa, String etapaCor,
                                  Entrevista entrevista) {
    }

    public record PaginaEntrevistas(List<LinhaEntrevista> itens, long total, Map<Entrevista, Long> totais, int pagina,
                                    int tamanho) {
    }

    /**
     * Uma página da lista de clientes. Busca, filtro e paginação são feitos no banco; só os clientes da página são analisados.
     * A exceção é o filtro "pendencia", que depende da análise (quem ainda não está apto).
     */
    @Transactional(readOnly = true)
    public Pagina listar(String q, String filtro, boolean arquivados, List<Etapa> etapas, Integer pagina, Integer tamanho) {
        int pg = ClienteConsulta.pagina(pagina);
        int tam = ClienteConsulta.tamanho(tamanho);
        Specification<Cliente> spec = Specification.where(ClienteConsulta.busca(q, arquivados))
            .and(ClienteConsulta.filtro(filtro)).and(ClienteConsulta.etapas(etapas));
        Sort ordem = Sort.by("nome").ascending().and(Sort.by("id"));
        if ("pendencia".equals(filtro)) {
            List<Analise> todos = analisar(clientes.findAll(spec, ordem)).stream().filter(a -> !a.apto()).toList();
            int de = Math.min(pg * tam, todos.size());
            return new Pagina(todos.subList(de, Math.min(de + tam, todos.size())), todos.size(), pg, tam);
        }
        Page<Cliente> p = clientes.findAll(spec, PageRequest.of(pg, tam, ordem));
        return new Pagina(analisar(p.getContent()), p.getTotalElements(), pg, tam);
    }

    /** Lista enxuta da tela Entrevistas (não precisa da análise completa). */
    @Transactional(readOnly = true)
    public PaginaEntrevistas entrevistas(String q, Entrevista situacao, Integer pagina, Integer tamanho) {
        int pg = ClienteConsulta.pagina(pagina);
        int tam = ClienteConsulta.tamanho(tamanho);
        Specification<Cliente> spec = Specification.where(ClienteConsulta.busca(q, false)).and(ClienteConsulta.entrevista(situacao));
        Page<Cliente> p = clientes.findAll(spec, PageRequest.of(pg, tam, Sort.by("nome").ascending().and(Sort.by("id"))));
        List<LinhaEntrevista> itens = p.getContent().stream().map(c -> new LinhaEntrevista(c.getId(), c.getNome(),
            c.getMunicipio(), c.getColonia(), c.getEtapa().getNome(), c.getEtapa().getCor(), c.getEntrevista())).toList();
        Map<Entrevista, Long> totais = new EnumMap<>(Entrevista.class);
        for (Entrevista e : Entrevista.values()) {
            totais.put(e, clientes.countByAtivoTrueAndEntrevista(e));
        }
        return new PaginaEntrevistas(itens, p.getTotalElements(), totais, pg, tam);
    }

    private static final int LOTE = 1000;

    private static <T> List<List<T>> lotes(List<T> itens) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < itens.size(); i += LOTE) {
            out.add(itens.subList(i, Math.min(i + LOTE, itens.size())));
        }
        return out;
    }

    /** Todos os clientes ativos de uma etapa, já analisados. */
    @Transactional(readOnly = true)
    public List<Analise> carregarEtapa(Etapa etapa) {
        Specification<Cliente> spec = Specification.where(ClienteConsulta.busca(null, false)).and(ClienteConsulta.filtro(etapa.name()));
        return analisar(clientes.findAll(spec, Sort.by("nome").ascending().and(Sort.by("id"))));
    }

    public List<Analise> analisarClientes(List<Cliente> lista) {
        return analisar(lista);
    }

    private List<Analise> analisar(List<Cliente> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> todosIds = lista.stream().map(Cliente::getId).toList();
        Map<Long, Map<Requisito, Boolean>> reqs = new HashMap<>();
        Map<Long, Set<TipoDocumento>> docs = new HashMap<>();
        Map<Long, Map<String, ClienteDados.DaeInfo>> daesPorCliente = new HashMap<>();
        Map<Long, BigDecimal> recebido = new HashMap<>();
        // Em lotes: o PostgreSQL aceita no máximo 32.767 parâmetros por consulta.
        for (List<Long> ids : lotes(todosIds)) {
            for (RequisitoCliente r : requisitos.findByClienteIdIn(ids)) {
                reqs.computeIfAbsent(r.getClienteId(), k -> new HashMap<>()).put(r.getChave(), r.getAtende());
            }
            for (Documento d : documentos.findByClienteIdIn(ids)) {
                docs.computeIfAbsent(d.getClienteId(), k -> EnumSet.noneOf(TipoDocumento.class)).add(d.getTipo());
            }
            for (Dae d : daes.findByClienteIdIn(ids)) {
                daesPorCliente.computeIfAbsent(d.getClienteId(), k -> new HashMap<>())
                    .put(d.getCompetencia(), new ClienteDados.DaeInfo(d.getStatus(), d.getValorVenda(), d.getValorDae()));
            }
            for (Honorario h : honorarios.findByClienteIdIn(ids)) {
                recebido.merge(h.getClienteId(), h.getValor(), BigDecimal::add);
            }
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
                c.getComplemento(), c.getBairro(), c.getUf(), c.getAuxiliarId(), c.getSenhaGovCifrada() != null,
                c.getCpfConferidoEm(), c.getCpfConferidoPor(), c.isCadastroConcluido()));
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
        c.setCadastroConcluido(!Boolean.TRUE.equals(f.emAndamento()));
        c = clientes.save(c);
        eventos.registrar(c.getId(), usuarioId, "cadastro", c.isCadastroConcluido()
            ? "Cliente cadastrado na plataforma" : "Cadastro iniciado (falta finalizar documentos e entrevista)");
        if (f.respostas() != null && !f.respostas().isEmpty()) {
            salvarRequisitos(c.getId(), Requisito.respostas(f.respostas()), usuarioId);
        }
        return c;
    }

    /** Marca o cadastro como finalizado (sai da aba "Falta finalizar"). */
    @Transactional
    public void concluirCadastro(Long id, Long usuarioId) {
        Cliente c = buscar(id);
        if (c.isCadastroConcluido()) {
            return;
        }
        c.setCadastroConcluido(true);
        c.setAtualizadoEm(hoje.agora());
        clientes.save(c);
        eventos.registrar(id, usuarioId, "cadastro", "Cadastro finalizado");
    }

    public long totalEmAndamento() {
        return clientes.countByAtivoTrueAndCadastroConcluidoFalse();
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

    /** Marca ou desfaz a conferência do CPF na Receita (consulta manual, sem custo). */
    @Transactional
    public void conferirCpf(Long id, boolean conferido, Long usuarioId) {
        Cliente c = buscar(id);
        c.setCpfConferidoEm(conferido ? hoje.data() : null);
        c.setCpfConferidoPor(conferido ? usuarioId : null);
        c.setAtualizadoEm(hoje.agora());
        clientes.save(c);
        eventos.registrar(id, usuarioId, "cpf", conferido ? "CPF conferido na Receita" : "Conferência do CPF desfeita");
        atualizarEtapaAutomatica(id, usuarioId);
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
        else if (!Telefone.valido(f.telefone())) erros.add("Telefone inválido: informe o DDD e o número (10 ou 11 números).");
        if (limpar(f.nit()) != null && f.nit().replaceAll("\\D", "").length() != 11) erros.add("O NIT/PIS tem 11 números.");
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
        if (c.getCpf() != null && !c.getCpf().equals(cpf)) {
            c.setCpfConferidoEm(null);       // CPF novo: a conferência anterior não vale mais
            c.setCpfConferidoPor(null);
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
        Cliente cliente = buscar(id);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraException("Escolha o arquivo.");
        }
        if (tipo == TipoDocumento.COMPROVANTE_DAE && (limpar(competencia) == null || !competencia.matches("\\d{4}-\\d{2}"))) {
            throw new RegraException("Escolha a competência do DAE pago.");
        }
        String nome = arquivos.salvar(id, arquivo);
        Documento d = new Documento();
        d.setClienteId(id);
        d.setTipo(tipo);
        d.setCompetencia(tipo == TipoDocumento.COMPROVANTE_DAE ? limpar(competencia) : null);
        String original = arquivo.getOriginalFilename() == null ? nome : arquivo.getOriginalFilename();
        d.setNomeOriginal(original.length() > 200 ? original.substring(0, 200) : original);
        d.setArquivo(nome);
        d.setTamanho(arquivo.getSize());
        d.setEnviadoPor(usuarioId);
        d.setEnviadoEm(hoje.agora());
        Documento salvo = documentos.save(d);
        String comp = salvo.getCompetencia();
        String desc = tipo.getNome() + " anexado";
        boolean daeValido = comp != null && cliente.getEntrevista() == Entrevista.CONFIRMADO
            && Datas.competenciasDae(hoje.data()).contains(comp);
        if (tipo == TipoDocumento.COMPROVANTE_DAE && comp != null && !daeValido) {
            desc += " (DAE não marcado: só vale para entrevista confirmada e competência desde maio)";
        }
        if (tipo == TipoDocumento.COMPROVANTE_DAE && daeValido) {
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
                if (tot > 12) {
                    throw new RegraException("O total de parcelas deve ser de 1 a 12.");
                }
                if (f.parcelasLiberadas() != null && (f.parcelasLiberadas() < 0 || f.parcelasLiberadas() > tot)) {
                    throw new RegraException("As parcelas liberadas devem ficar entre 0 e o total (" + tot + ").");
                }
                int lib = f.parcelasLiberadas() == null ? 0 : f.parcelasLiberadas();
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
                LocalDate ciencia = Datas.ler(f.ciencia());
                if (limpar(f.ciencia()) != null && ciencia == null) {
                    throw new RegraException("Data de ciência inválida. Use dd/mm/aaaa.");
                }
                if (ciencia != null && ciencia.isAfter(hoje.data())) {
                    throw new RegraException("A data de ciência não pode ser futura.");
                }
                c.setCienciaData(ciencia);
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
        if (Dinheiro.invalido(valorTxt)) {
            throw new RegraException("Valor inválido: use só números, por exemplo 486,30.");
        }
        BigDecimal valor = Dinheiro.ler(valorTxt);
        if (valor == null) {
            valor = a.honorariosPorParcela();
        }
        if (valor.signum() <= 0) {
            throw new RegraException("Informe um valor maior que zero.");
        }
        LocalDate data = Datas.ler(dataTxt);
        if (limpar(dataTxt) != null && data == null) {
            throw new RegraException("Data do recebimento inválida. Use dd/mm/aaaa ou deixe vazio para hoje.");
        }
        if (data != null && data.isAfter(hoje.data())) {
            throw new RegraException("A data do recebimento não pode ser futura.");
        }
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
