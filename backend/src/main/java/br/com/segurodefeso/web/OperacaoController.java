package br.com.segurodefeso.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.ClienteDados;
import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.dominio.Cpf;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Dinheiro;
import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.FilaDoDia;
import br.com.segurodefeso.dominio.Mensagens;
import br.com.segurodefeso.dominio.Prazos;
import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.dominio.StatusDae;
import br.com.segurodefeso.dominio.Triagem;
import br.com.segurodefeso.entidade.Cliente;
import br.com.segurodefeso.entidade.Dae;
import br.com.segurodefeso.entidade.Defeso;
import br.com.segurodefeso.entidade.RequisitoCliente;
import br.com.segurodefeso.repositorio.ClienteRepo;
import br.com.segurodefeso.repositorio.DaeRepo;
import br.com.segurodefeso.repositorio.DefesoRepo;
import br.com.segurodefeso.repositorio.RequisitoRepo;
import br.com.segurodefeso.servico.ClienteService;
import br.com.segurodefeso.servico.ConfigService;
import br.com.segurodefeso.servico.EventoService;
import br.com.segurodefeso.servico.RegraException;
import br.com.segurodefeso.servico.UsuarioAtual;

/** Início, triagem, DAE, prazos, defesos, mensagens, honorários e relatórios. */
@RestController
@RequestMapping("/api")
public class OperacaoController {

    public record TriagemReq(Map<String, Boolean> respostas) {
    }

    public record DaeItem(Long clienteId, String venda, String status) {
    }

    public record DaeReq(String competencia, List<DaeItem> itens) {
    }

    public record DefesoReq(String especie, String regiao, String portaria, String inicio, String fim, String prazoRequerimento) {
    }

    private final ClienteService clientes;
    private final ClienteRepo clienteRepo;
    private final RequisitoRepo requisitos;
    private final DaeRepo daes;
    private final DefesoRepo defesos;
    private final ConfigService config;
    private final EventoService eventos;
    private final UsuarioAtual atual;
    private final Hoje hoje;

    public OperacaoController(ClienteService clientes, ClienteRepo clienteRepo, RequisitoRepo requisitos, DaeRepo daes,
                              DefesoRepo defesos, ConfigService config, EventoService eventos, UsuarioAtual atual, Hoje hoje) {
        this.clientes = clientes;
        this.clienteRepo = clienteRepo;
        this.requisitos = requisitos;
        this.daes = daes;
        this.defesos = defesos;
        this.config = config;
        this.eventos = eventos;
        this.atual = atual;
        this.hoje = hoje;
    }

    // ================================================================== início
    @GetMapping("/inicio")
    public Map<String, Object> inicio() {
        List<Analise> todos = clientes.carregar(false);
        LocalDate h = hoje.data();
        List<FilaDoDia.Item> fila = FilaDoDia.calcular(todos, h);
        Map<Etapa, Long> cont = contagem(todos);
        String comp = Datas.competenciasAte(h, 1).get(0);
        long daeAberto = todos.stream().filter(a -> a.cliente().entrevista() == Entrevista.CONFIRMADO
            && a.cliente().daes().get(comp) == null).count();
        LocalDate exig = todos.stream().filter(a -> a.cliente().etapa() == Etapa.EXIGENCIA && a.cliente().prazoData() != null)
            .map(a -> a.cliente().prazoData()).sorted().findFirst().orElse(null);
        List<Map<String, Object>> kpis = new ArrayList<>();
        kpis.add(kpi("Clientes ativos", String.valueOf(todos.size()), cont.get(Etapa.DOCUMENTACAO) + " em documentação", "/clientes"));
        kpis.add(kpi("Ações para hoje", String.valueOf(fila.size()),
            fila.stream().filter(f -> f.prioridade().equals("Urgente")).count() + " urgente(s)", "#fila"));
        kpis.add(kpi("Aptos a requerer", String.valueOf(todos.stream().filter(a -> a.cliente().etapa() == Etapa.A_REQUERER && a.apto()).count()),
            todos.stream().filter(a -> !a.apto() && (a.cliente().etapa() == Etapa.DOCUMENTACAO || a.cliente().etapa() == Etapa.A_REQUERER)).count()
                + " com pendência", "/requerimentos"));
        kpis.add(kpi("Em exigência", String.valueOf(cont.get(Etapa.EXIGENCIA)),
            exig != null ? "Prazo mais próximo: " + Datas.br(exig) : "Nenhuma exigência aberta", "/requerimentos"));
        kpis.add(kpi("DAE do mês em aberto", String.valueOf(daeAberto), Datas.competenciaLabel(comp), "/dae"));
        if (atual.gestor()) {
            BigDecimal saldo = todos.stream().map(Analise::honorariosSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            kpis.add(kpi("Honorários a receber", Dinheiro.brl(saldo), "parcelas já liberadas", "/honorarios"));
        }
        long max = Math.max(1, cont.values().stream().mapToLong(Long::longValue).max().orElse(1));
        List<Map<String, Object>> funil = new ArrayList<>();
        for (Etapa e : Etapa.values()) {
            funil.add(Map.<String, Object>of("etapa", e.getNome(), "qtd", cont.get(e), "pct", Math.round(cont.get(e) * 100.0 / max), "cor", e.getCor()));
        }
        return Map.of("kpis", kpis, "fila", fila, "funil", funil);
    }

    private static Map<String, Object> kpi(String rotulo, String valor, String dica, String link) {
        return Map.<String, Object>of("rotulo", rotulo, "valor", valor, "dica", dica, "link", link);
    }

    private static Map<Etapa, Long> contagem(List<Analise> todos) {
        Map<Etapa, Long> cont = new EnumMap<>(Etapa.class);
        for (Etapa e : Etapa.values()) cont.put(e, 0L);
        todos.forEach(a -> cont.merge(a.cliente().etapa(), 1L, Long::sum));
        return cont;
    }

    // ================================================================== triagem
    private static Map<Requisito, Boolean> respostas(Map<String, Boolean> m) {
        Map<Requisito, Boolean> r = new HashMap<>();
        if (m != null) m.forEach((k, v) -> r.put(Requisito.valueOf(k), v));
        return r;
    }

    @PostMapping("/triagem/avaliar")
    public Triagem.Resultado avaliar(@RequestBody TriagemReq req) {
        return Triagem.avaliar(respostas(req.respostas()));
    }

    // ================================================================== DAE
    @GetMapping("/dae")
    public Map<String, Object> dae(@RequestParam(required = false) String competencia) {
        List<String> comps = Datas.competenciasDae(hoje.data());
        String comp = competencia != null && comps.contains(competencia) ? competencia : comps.get(comps.size() - 1);
        ConfigValores cfg = config.valores();
        List<Analise> lista = clientes.carregar(false).stream()
            .filter(a -> a.cliente().entrevista() == Entrevista.CONFIRMADO).toList();
        Map<String, Long> resumo = new LinkedHashMap<>();
        for (StatusDae s : StatusDae.values()) {
            resumo.put(s.name(), lista.stream().filter(a -> {
                ClienteDados.DaeInfo d = a.cliente().daes().get(comp);
                return d != null && d.status() == s;
            }).count());
        }
        resumo.put("EM_ABERTO", lista.stream().filter(a -> a.cliente().daes().get(comp) == null).count());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("competencias", comps);
        m.put("competencia", comp);
        m.put("vencimento", Datas.vencimentoDae(comp, cfg.diaVencimentoDae()));
        m.put("aliquota", cfg.aliquotaDae());
        m.put("resumo", resumo);
        m.put("clientes", lista);
        return m;
    }

    @PostMapping("/dae")
    @Transactional
    public Map<String, String> salvarDae(@RequestBody DaeReq req) {
        List<String> comps = Datas.competenciasDae(hoje.data());
        if (req.competencia() == null || !comps.contains(req.competencia())) {
            throw new RegraException("Competência inválida.");
        }
        ConfigValores cfg = config.valores();
        int n = 0;
        for (DaeItem item : req.itens() == null ? List.<DaeItem>of() : req.itens()) {
            BigDecimal venda = Dinheiro.ler(item.venda());
            StatusDae st = item.status() == null || item.status().isBlank() ? null : StatusDae.valueOf(item.status());
            Dae atualDae = daes.findByClienteIdAndCompetencia(item.clienteId(), req.competencia()).orElse(null);
            if (venda == null && st == null && atualDae == null) continue;
            Cliente cli = clienteRepo.findById(item.clienteId()).orElseThrow(() -> new RegraException("Cliente não encontrado."));
            if (cli.getEntrevista() != Entrevista.CONFIRMADO) {
                throw new RegraException("DAE só para clientes com entrevista confirmada: " + cli.getNome() + ".");
            }
            if (st == null && atualDae == null) {
                throw new RegraException("Escolha o status (Pago ou Declarado) de " + cli.getNome() + ".");
            }
            if (st == null) st = atualDae.getStatus();
            if (atualDae != null && st == atualDae.getStatus() && iguais(venda, atualDae.getValorVenda())) continue;
            Dae d = atualDae != null ? atualDae : new Dae();
            d.setClienteId(item.clienteId());
            d.setCompetencia(req.competencia());
            d.setValorVenda(venda);
            d.setValorDae(venda == null ? null : venda.multiply(cfg.aliquotaDae()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            d.setStatus(st);
            d.setAtualizadoEm(hoje.agora());
            daes.save(d);
            eventos.registrar(item.clienteId(), atual.id(), "dae", "DAE " + Datas.competenciaLabel(req.competencia()) + ": "
                + (venda != null ? "venda " + Dinheiro.brl(venda) + ", " : "") + st.getNome().toLowerCase());
            n++;
        }
        return Map.of("mensagem", n + " lançamento(s) de DAE salvo(s) para " + Datas.competenciaLabel(req.competencia()) + ".");
    }

    private static boolean iguais(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    // ================================================================== prazos e defesos
    private List<Prazos.DefesoInfo> defesosInfo() {
        return defesos.findAll().stream().map(d -> new Prazos.DefesoInfo(d.getEspecie(), d.getRegiao(), d.getPortaria(),
            d.getInicio(), d.getFim(), d.getPrazoRequerimento())).toList();
    }

    @GetMapping("/prazos")
    public List<Prazos.Prazo> prazos(@RequestParam(defaultValue = "Todos") String tipo) {
        List<Prazos.Prazo> l = Prazos.calcular(clientes.carregar(false), defesosInfo(), config.valores(), hoje.data());
        return tipo.equals("Todos") ? l : l.stream().filter(p -> p.tipo().equals(tipo)).toList();
    }

    @GetMapping("/defesos")
    public List<Defeso> listarDefesos() {
        return defesos.findAllByOrderByInicioDesc();
    }

    @PostMapping("/defesos")
    public Map<String, String> criarDefeso(@RequestBody DefesoReq req) {
        LocalDate inicio = Datas.ler(req.inicio());
        LocalDate prazo = Datas.ler(req.prazoRequerimento());
        if (req.especie() == null || req.especie().isBlank() || inicio == null || prazo == null) {
            throw new RegraException("Informe a espécie, o início do defeso e o prazo final para requerer (datas em dd/mm/aaaa).");
        }
        Defeso d = new Defeso();
        d.setEspecie(req.especie().trim());
        d.setRegiao(vazioNulo(req.regiao()));
        d.setPortaria(vazioNulo(req.portaria()));
        d.setInicio(inicio);
        d.setFim(Datas.ler(req.fim()));
        d.setPrazoRequerimento(prazo);
        d.setCriadoEm(hoje.agora());
        defesos.save(d);
        return Map.of("mensagem", "Defeso de " + d.getEspecie() + " cadastrado. Os prazos já aparecem na tela Prazos.");
    }

    @DeleteMapping("/defesos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> excluirDefeso(@PathVariable Long id) {
        defesos.deleteById(id);
        return Map.of("mensagem", "Defeso excluído.");
    }

    private static String vazioNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ================================================================== mensagens
    @GetMapping("/mensagens")
    public Map<String, Object> mensagem(@RequestParam Long cliente, @RequestParam(defaultValue = "DAE") String modelo) {
        Analise a = clientes.carregar(cliente);
        String texto = Mensagens.texto(Mensagens.Modelo.valueOf(modelo), a, config.valores());
        Map<String, Object> m = new HashMap<>();
        m.put("texto", texto);
        m.put("link", Mensagens.linkWhatsapp(a.cliente().telefone(), texto));
        m.put("telefone", a.cliente().telefone());
        return m;
    }

    // ================================================================== honorários e relatórios
    @GetMapping("/honorarios")
    public Map<String, Object> honorarios() {
        List<Analise> l = clientes.carregar(false).stream()
            .filter(a -> a.cliente().etapa() == Etapa.DEFERIDO && a.cliente().parcelasTotal() > 0).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientes", l);
        m.put("saldo", l.stream().map(Analise::honorariosSaldo).reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("recebido", l.stream().map(Analise::honorariosRecebidos).reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("futuro", l.stream().map(Analise::honorariosFuturos).reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("salarioMinimo", config.valores().salarioMinimo());
        return m;
    }

    @GetMapping("/relatorios")
    public Map<String, Object> relatorios() {
        List<Analise> todos = clientes.carregar(false);
        Map<String, long[]> cols = new LinkedHashMap<>();
        Map<String, String> municipios = new HashMap<>();
        for (Analise a : todos) {
            String k = a.cliente().colonia() == null ? "Sem colônia" : a.cliente().colonia();
            long[] v = cols.computeIfAbsent(k, x -> new long[3]);
            v[0]++;
            if (a.cliente().etapa() == Etapa.DEFERIDO) v[1]++;
            if (a.cliente().etapa() == Etapa.INDEFERIDO) v[2]++;
            municipios.putIfAbsent(k, a.cliente().municipio() == null ? "" : a.cliente().municipio());
        }
        long max = Math.max(1, cols.values().stream().mapToLong(v -> v[0]).max().orElse(1));
        List<Map<String, Object>> colonias = cols.entrySet().stream()
            .sorted((x, y) -> Long.compare(y.getValue()[0], x.getValue()[0]))
            .map(e -> {
                long dec = e.getValue()[1] + e.getValue()[2];
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("colonia", e.getKey());
                m.put("municipio", municipios.get(e.getKey()));
                m.put("clientes", e.getValue()[0]);
                m.put("pct", Math.round(e.getValue()[0] * 100.0 / max));
                m.put("deferimento", dec == 0 ? "sem decisão" : Math.round(e.getValue()[1] * 100.0 / dec) + "% def.");
                return m;
            }).toList();
        Map<Etapa, Long> cont = contagem(todos);
        long dec = cont.get(Etapa.DEFERIDO) + cont.get(Etapa.INDEFERIDO);
        long totalDae = todos.stream().mapToLong(a -> a.cliente().daes().size()).sum();
        long pagos = todos.stream().flatMap(a -> a.cliente().daes().values().stream()).filter(d -> d.status() == StatusDae.PAGO).count();
        List<Map<String, Object>> ind = List.of(
            Map.<String, Object>of("rotulo", "Índice de deferimento", "valor", dec == 0 ? "—" : Math.round(cont.get(Etapa.DEFERIDO) * 100.0 / dec) + "%"),
            Map.<String, Object>of("rotulo", "DAE pagos (todos os lançados)", "valor", totalDae == 0 ? "—" : Math.round(pagos * 100.0 / totalDae) + "%"),
            Map.<String, Object>of("rotulo", "Clientes com pendência", "valor", String.valueOf(todos.stream().filter(a -> !a.apto()
                && (a.cliente().etapa() == Etapa.DOCUMENTACAO || a.cliente().etapa() == Etapa.A_REQUERER)).count())),
            Map.<String, Object>of("rotulo", "Procurações e-CAC válidas", "valor", todos.stream().filter(Analise::procuracaoValida).count() + " de " + todos.size()));
        Map<String, Long> motivos = new LinkedHashMap<>();
        todos.stream().filter(a -> a.cliente().etapa() == Etapa.DOCUMENTACAO || a.cliente().etapa() == Etapa.A_REQUERER)
            .flatMap(a -> a.bloqueios().stream())
            .map(b -> b.split(":")[0].replaceAll("^\\d+ ", "").trim())
            .forEach(b -> motivos.merge(b, 1L, Long::sum));
        List<Map<String, Object>> mot = motivos.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
            .map(e -> Map.<String, Object>of("motivo", e.getKey(), "qtd", e.getValue())).toList();
        return Map.of("colonias", colonias, "indicadores", ind, "motivos", mot);
    }
}
