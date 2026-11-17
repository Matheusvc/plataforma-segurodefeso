package br.com.segurodefeso.servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Dinheiro;
import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.FilaDoDia;

/** Números e fila da tela Início. */
@Service
public class InicioService {

    /** A fila do dia mostra as mais urgentes; o total aparece no indicador. */
    static final int LIMITE_FILA = 30;

    private final ClienteService clientes;
    private final UsuarioAtual atual;
    private final Hoje hoje;

    public InicioService(ClienteService clientes, UsuarioAtual atual, Hoje hoje) {
        this.clientes = clientes;
        this.atual = atual;
        this.hoje = hoje;
    }

    public Map<String, Object> painel() {
        List<Analise> todos = clientes.carregar(false);
        LocalDate h = hoje.data();
        List<FilaDoDia.Item> fila = FilaDoDia.calcular(todos, h);
        Map<Etapa, Long> cont = Etapa.contar(todos);
        String comp = Datas.competenciasAte(h, 1).get(0);
        long daeAberto = todos.stream().filter(a -> a.cliente().entrevista() == Entrevista.CONFIRMADO
            && a.cliente().daes().get(comp) == null).count();
        LocalDate exigencia = todos.stream().filter(a -> a.cliente().etapa() == Etapa.EXIGENCIA && a.cliente().prazoData() != null)
            .map(a -> a.cliente().prazoData()).sorted().findFirst().orElse(null);
        long aptos = todos.stream().filter(a -> a.cliente().etapa() == Etapa.A_REQUERER && a.apto()).count();
        long comPendencia = todos.stream().filter(a -> !a.apto() && a.pendenteDeRequerer()).count();

        List<Map<String, Object>> kpis = new ArrayList<>();
        kpis.add(kpi("Clientes ativos", String.valueOf(todos.size()), cont.get(Etapa.DOCUMENTACAO) + " em documentação", "/clientes"));
        kpis.add(kpi("Ações para hoje", String.valueOf(fila.size()),
            fila.stream().filter(f -> f.prioridade().equals("Urgente")).count() + " urgente(s)", "#fila"));
        kpis.add(kpi("Aptos a requerer", String.valueOf(aptos), comPendencia + " com pendência", "/requerimentos"));
        kpis.add(kpi("Em exigência", String.valueOf(cont.get(Etapa.EXIGENCIA)),
            exigencia != null ? "Prazo mais próximo: " + Datas.br(exigencia) : "Nenhuma exigência aberta", "/requerimentos"));
        kpis.add(kpi("DAE do mês em aberto", String.valueOf(daeAberto), Datas.competenciaLabel(comp), "/dae"));
        if (atual.gestor()) {
            BigDecimal saldo = todos.stream().map(Analise::honorariosSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            kpis.add(kpi("Honorários a receber", Dinheiro.brl(saldo), "parcelas já liberadas", "/honorarios"));
        }

        long max = Math.max(1, cont.values().stream().mapToLong(Long::longValue).max().orElse(1));
        List<Map<String, Object>> funil = new ArrayList<>();
        for (Etapa e : Etapa.values()) {
            funil.add(Map.of("etapa", e.getNome(), "qtd", cont.get(e), "pct", Math.round(cont.get(e) * 100.0 / max), "cor", e.getCor()));
        }
        List<FilaDoDia.Item> filaVisivel = fila.size() > LIMITE_FILA ? fila.subList(0, LIMITE_FILA) : fila;
        return Map.of("kpis", kpis, "fila", filaVisivel, "filaTotal", fila.size(), "funil", funil);
    }

    private static Map<String, Object> kpi(String rotulo, String valor, String dica, String link) {
        return Map.of("rotulo", rotulo, "valor", valor, "dica", dica, "link", link);
    }
}
