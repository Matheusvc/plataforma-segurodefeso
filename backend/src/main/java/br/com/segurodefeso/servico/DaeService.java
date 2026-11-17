package br.com.segurodefeso.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Dinheiro;
import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.StatusDae;
import br.com.segurodefeso.entidade.Cliente;
import br.com.segurodefeso.entidade.Dae;
import br.com.segurodefeso.repositorio.ClienteRepo;
import br.com.segurodefeso.repositorio.DaeRepo;

/** Lançamento manual do DAE (competências desde maio, só clientes com entrevista confirmada). */
@Service
public class DaeService {

    public record Item(Long clienteId, String venda, String status) {
    }

    public record Lancamento(String competencia, List<Item> itens) {
    }

    private final ClienteService clientes;
    private final ClienteRepo clienteRepo;
    private final DaeRepo daes;
    private final ConfigService config;
    private final EventoService eventos;
    private final UsuarioAtual atual;
    private final Hoje hoje;

    public DaeService(ClienteService clientes, ClienteRepo clienteRepo, DaeRepo daes, ConfigService config,
                      EventoService eventos, UsuarioAtual atual, Hoje hoje) {
        this.clientes = clientes;
        this.clienteRepo = clienteRepo;
        this.daes = daes;
        this.config = config;
        this.eventos = eventos;
        this.atual = atual;
        this.hoje = hoje;
    }

    /**
     * Dados da tela DAE para a competência pedida (padrão: a mais recente): uma página dos clientes com entrevista
     * confirmada e os totais da competência, contados no banco.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> tela(String competencia, String q, Integer pagina, Integer tamanho) {
        List<String> comps = Datas.competenciasDae(hoje.data());
        String comp = competencia != null && comps.contains(competencia) ? competencia : comps.get(comps.size() - 1);
        ConfigValores cfg = config.valores();
        int pg = ClienteConsulta.pagina(pagina);
        int tam = ClienteConsulta.tamanho(tamanho);
        Specification<Cliente> spec = Specification.where(ClienteConsulta.busca(q, false)).and(ClienteConsulta.entrevista(Entrevista.CONFIRMADO));
        Page<Cliente> p = clienteRepo.findAll(spec, PageRequest.of(pg, tam, Sort.by("nome").ascending().and(Sort.by("id"))));
        List<Analise> lista = clientes.analisarClientes(p.getContent());

        long confirmados = clienteRepo.countByAtivoTrueAndEntrevista(Entrevista.CONFIRMADO);
        Map<String, Long> resumo = new LinkedHashMap<>();
        long lancados = 0;
        for (StatusDae s : StatusDae.values()) {
            long n = daes.contarConfirmados(comp, s);
            resumo.put(s.name(), n);
            lancados += n;
        }
        resumo.put("EM_ABERTO", Math.max(0, confirmados - lancados));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("competencias", comps);
        m.put("competencia", comp);
        m.put("vencimento", Datas.vencimentoDae(comp, cfg.diaVencimentoDae()));
        m.put("aliquota", cfg.aliquotaDae());
        m.put("resumo", resumo);
        m.put("clientes", lista);
        m.put("total", p.getTotalElements());
        m.put("pagina", pg);
        m.put("tamanho", tam);
        return m;
    }

    @Transactional
    public String salvar(Lancamento req) {
        if (req.competencia() == null || !Datas.competenciasDae(hoje.data()).contains(req.competencia())) {
            throw new RegraException("Competência inválida.");
        }
        ConfigValores cfg = config.valores();
        int salvos = 0;
        for (Item item : req.itens() == null ? List.<Item>of() : req.itens()) {
            if (gravar(item, req.competencia(), cfg)) {
                salvos++;
            }
        }
        return salvos + " lançamento(s) de DAE salvo(s) para " + Datas.competenciaLabel(req.competencia()) + ".";
    }

    /** Devolve true se algo mudou. */
    private boolean gravar(Item item, String competencia, ConfigValores cfg) {
        if (Dinheiro.invalido(item.venda())) {
            String nome = clienteRepo.findById(item.clienteId()).map(Cliente::getNome).orElse("cliente");
            throw new RegraException("Venda inválida de " + nome + ": use só números, por exemplo 1.250,00.");
        }
        BigDecimal venda = Dinheiro.ler(item.venda());
        StatusDae status = item.status() == null || item.status().isBlank() ? null : StatusDae.valueOf(item.status());
        Dae existente = daes.findByClienteIdAndCompetencia(item.clienteId(), competencia).orElse(null);
        if (venda == null && status == null && existente == null) {
            return false;
        }
        Cliente cliente = clienteRepo.findById(item.clienteId()).orElseThrow(() -> new RegraException("Cliente não encontrado."));
        if (cliente.getEntrevista() != Entrevista.CONFIRMADO) {
            throw new RegraException("DAE só para clientes com entrevista confirmada: " + cliente.getNome() + ".");
        }
        if (status == null && existente == null) {
            throw new RegraException("Escolha o status (Pago ou Declarado) de " + cliente.getNome() + ".");
        }
        if (status == null) {
            status = existente.getStatus();
        }
        if (existente != null && status == existente.getStatus() && iguais(venda, existente.getValorVenda())) {
            return false;
        }
        Dae d = existente != null ? existente : new Dae();
        d.setClienteId(item.clienteId());
        d.setCompetencia(competencia);
        d.setValorVenda(venda);
        d.setValorDae(venda == null ? null : venda.multiply(cfg.aliquotaDae()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        d.setStatus(status);
        d.setAtualizadoEm(hoje.agora());
        daes.save(d);
        eventos.registrar(item.clienteId(), atual.id(), "dae", "DAE " + Datas.competenciaLabel(competencia) + ": "
            + (venda != null ? "venda " + Dinheiro.brl(venda) + ", " : "") + status.getNome().toLowerCase());
        return true;
    }

    private static boolean iguais(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
