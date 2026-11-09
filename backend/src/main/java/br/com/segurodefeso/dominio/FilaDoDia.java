package br.com.segurodefeso.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Gera a fila de trabalho do dia, por prioridade, a partir dos clientes analisados. */
public final class FilaDoDia {

    public record Item(String prioridade, String cor, String titulo, String detalhe, String prazo, String acao, String url) {
    }

    private static final Map<String, String> COR = Map.of("Urgente", "erro", "Alta", "aviso", "Média", "info", "Rotina", "neutro");
    private static final Map<String, Integer> ORDEM = Map.of("Urgente", 0, "Alta", 1, "Média", 2, "Rotina", 3);

    private FilaDoDia() {
    }

    public static List<Item> calcular(List<Analise> clientes, LocalDate hoje) {
        List<Item> fila = new ArrayList<>();
        int naoConferidos = 0;
        for (Analise a : clientes) {
            ClienteDados c = a.cliente();
            String ficha = "/clientes/" + c.id();
            if (c.etapa() == Etapa.EXIGENCIA) {
                Long n = Datas.dias(c.prazoData(), hoje);
                add(fila, "Urgente", c.nome() + ": cumprir exigência", Prazos.nvl(c.exigenciaTexto(), "Ver exigência no site."),
                        c.prazoData() != null ? "até " + Datas.br(c.prazoData()) + " · " + Datas.faltaTxt(n) : "prazo não informado",
                        "Abrir", ficha + "?aba=documentos");
            }
            if (c.etapa() == Etapa.INDEFERIDO) {
                add(fila, "Urgente", c.nome() + ": decidir recurso ou ação", Prazos.nvl(c.indeferimentoMotivo(), "Ver motivo no site."),
                        "conferir prazo", "Abrir", ficha + "?aba=linha");
            }
            if (!a.daeVencidos().isEmpty()) {
                add(fila, "Alta", c.nome() + ": DAE vencido (" + labels(a.daeVencidos()) + ")",
                        a.procuracaoValida() ? "Emitir nova guia com acréscimos e cobrar o cliente."
                                : "Sem procuração e-CAC válida: emitir com o acesso do cliente, com acréscimos.",
                        "vencido", "Abrir", ficha + "?aba=dae");
            } else {
                List<Analise.DaeAberto> breve = a.daeAbertos().stream().filter(d -> d.dias() <= 15).toList();
                if (!breve.isEmpty()) {
                    add(fila, "Alta", c.nome() + ": DAE " + labels(breve) + " em aberto",
                            "Cobrar o pagamento. DAE em aberto pode impedir o seguro.", "vence em breve", "Cobrar",
                            "/mensagens?cliente=" + c.id() + "&modelo=DAE");
                }
            }
            if (c.etapa() == Etapa.A_REQUERER && a.apto()) {
                add(fila, "Média", c.nome() + ": fazer o pedido do seguro",
                        "Apto. Fazer o pedido no Emprega Brasil e registrar o protocolo.", "no prazo do defeso", "Registrar",
                        "/status?cliente=" + c.id());
            }
            if ((c.etapa() == Etapa.DOCUMENTACAO || c.etapa() == Etapa.A_REQUERER) && !a.apto()) {
                add(fila, "Média", c.nome() + ": pendências para requerer", String.join("; ", a.bloqueios()) + ".",
                        "regularizar", "Abrir", ficha + "?aba=requisitos");
            }
            if (a.procuracaoDias() != null && a.procuracaoDias() >= 0 && a.procuracaoDias() <= 30) {
                add(fila, "Média", c.nome() + ": procuração e-CAC vence", "Pedir ao cliente a renovação no e-CAC.",
                        "até " + Datas.br(c.procuracaoValidade()), "Abrir", ficha);
            }
            if (a.rgpDias() != null && a.rgpDias() <= 30 && c.etapa() != Etapa.DEFERIDO) {
                add(fila, "Média", c.nome() + ": RGP " + (a.rgpDias() < 0 ? "vencido" : "vence"),
                        "Renovar o RGP para não travar o pedido.", "até " + Datas.br(c.rgpValidade()), "Abrir", ficha);
            }
            if ((c.etapa() == Etapa.REQUERIDO || c.etapa() == Etapa.EXIGENCIA) && !hoje.equals(c.ultimaConferencia())) {
                naoConferidos++;
            }
        }
        if (naoConferidos > 0) {
            add(fila, "Rotina", "Conferir o status de " + naoConferidos + " pedido(s) no Emprega Brasil",
                    "Abrir o site e responder as perguntas da tela Atualizar status.", "hoje", "Conferir", "/status");
        }
        fila.sort(Comparator.comparingInt(i -> ORDEM.get(i.prioridade())));
        return fila;
    }

    private static String labels(List<Analise.DaeAberto> l) {
        return String.join(", ", l.stream().map(Analise.DaeAberto::label).toList());
    }

    private static void add(List<Item> fila, String p, String titulo, String detalhe, String prazo, String acao, String url) {
        fila.add(new Item(p, COR.get(p), titulo, detalhe, prazo, acao, url));
    }
}
