package br.com.segurodefeso.servico;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.StatusDae;

/** Honorários e relatórios gerenciais (restritos a administrador e advogado). */
@Service
public class RelatorioService {

    private final ClienteService clientes;
    private final ConfigService config;

    public RelatorioService(ClienteService clientes, ConfigService config) {
        this.clientes = clientes;
        this.config = config;
    }

    public Map<String, Object> honorarios() {
        List<Analise> deferidos = clientes.carregar(false).stream()
            .filter(a -> a.cliente().etapa() == Etapa.DEFERIDO && a.cliente().parcelasTotal() > 0).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientes", deferidos);
        m.put("saldo", somar(deferidos, Analise::honorariosSaldo));
        m.put("recebido", somar(deferidos, Analise::honorariosRecebidos));
        m.put("futuro", somar(deferidos, Analise::honorariosFuturos));
        m.put("salarioMinimo", config.valores().salarioMinimo());
        return m;
    }

    public Map<String, Object> relatorios() {
        List<Analise> todos = clientes.carregar(false);
        return Map.of("colonias", colonias(todos), "indicadores", indicadores(todos), "motivos", motivos(todos));
    }

    private static BigDecimal somar(List<Analise> lista, java.util.function.Function<Analise, BigDecimal> campo) {
        return lista.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Clientes e índice de deferimento por colônia, da maior para a menor. */
    private static List<Map<String, Object>> colonias(List<Analise> todos) {
        Map<String, long[]> totais = new LinkedHashMap<>();   // [clientes, deferidos, indeferidos]
        Map<String, String> municipios = new HashMap<>();
        for (Analise a : todos) {
            String colonia = a.cliente().colonia() == null ? "Sem colônia" : a.cliente().colonia();
            long[] v = totais.computeIfAbsent(colonia, x -> new long[3]);
            v[0]++;
            if (a.cliente().etapa() == Etapa.DEFERIDO) v[1]++;
            if (a.cliente().etapa() == Etapa.INDEFERIDO) v[2]++;
            municipios.putIfAbsent(colonia, a.cliente().municipio() == null ? "" : a.cliente().municipio());
        }
        long max = Math.max(1, totais.values().stream().mapToLong(v -> v[0]).max().orElse(1));
        return totais.entrySet().stream()
            .sorted((x, y) -> Long.compare(y.getValue()[0], x.getValue()[0]))
            .map(e -> {
                long decididos = e.getValue()[1] + e.getValue()[2];
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("colonia", e.getKey());
                m.put("municipio", municipios.get(e.getKey()));
                m.put("clientes", e.getValue()[0]);
                m.put("pct", Math.round(e.getValue()[0] * 100.0 / max));
                m.put("deferimento", decididos == 0 ? "sem decisão" : Math.round(e.getValue()[1] * 100.0 / decididos) + "% def.");
                return m;
            }).toList();
    }

    private static List<Map<String, Object>> indicadores(List<Analise> todos) {
        Map<Etapa, Long> cont = Etapa.contar(todos);
        long decididos = cont.get(Etapa.DEFERIDO) + cont.get(Etapa.INDEFERIDO);
        long lancados = todos.stream().mapToLong(a -> a.cliente().daes().size()).sum();
        long pagos = todos.stream().flatMap(a -> a.cliente().daes().values().stream()).filter(d -> d.status() == StatusDae.PAGO).count();
        long comPendencia = todos.stream().filter(a -> !a.apto() && a.pendenteDeRequerer()).count();
        return List.of(
            indicador("Índice de deferimento", decididos == 0 ? "—" : Math.round(cont.get(Etapa.DEFERIDO) * 100.0 / decididos) + "%"),
            indicador("DAE pagos (dos lançados)", lancados == 0 ? "—" : Math.round(pagos * 100.0 / lancados) + "%"),
            indicador("Clientes com pendência", String.valueOf(comPendencia)),
            indicador("Procurações e-CAC válidas", todos.stream().filter(Analise::procuracaoValida).count() + " de " + todos.size()));
    }

    private static Map<String, Object> indicador(String rotulo, String valor) {
        return Map.of("rotulo", rotulo, "valor", valor);
    }

    /** Pendências mais comuns entre quem ainda não pode requerer. */
    private static List<Map<String, Object>> motivos(List<Analise> todos) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        todos.stream().filter(Analise::pendenteDeRequerer)
            .flatMap(a -> a.bloqueios().stream())
            .map(b -> b.split(":")[0].replaceAll("^\\d+ ", "").trim())
            .forEach(b -> contagem.merge(b, 1L, Long::sum));
        return contagem.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
            .map(e -> Map.<String, Object>of("motivo", e.getKey(), "qtd", e.getValue())).toList();
    }
}
