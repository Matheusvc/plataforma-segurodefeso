package br.com.segurodefeso.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Vencimentos calculados a partir dos dados: exigência, recurso, RGP, procuração e defeso (o DAE tem tela própria). */
public final class Prazos {

    public record Prazo(LocalDate data, String dataBr, String tipo, String cliente, Long clienteId, String texto,
                        Long dias, String falta, String cor) {
    }

    public record DefesoInfo(String especie, String regiao, String portaria, LocalDate inicio, LocalDate fim,
                             LocalDate prazoRequerimento) {
    }

    private Prazos() {
    }

    static String corPorDias(Long n) {
        if (n == null || n <= 15) {
            return "erro";
        }
        return n <= 30 ? "aviso" : "neutro";
    }

    public static List<Prazo> calcular(List<Analise> clientes, List<DefesoInfo> defesos, ConfigValores cfg, LocalDate hoje) {
        List<Prazo> out = new ArrayList<>();
        for (Analise a : clientes) {
            ClienteDados c = a.cliente();
            if (c.etapa() == Etapa.EXIGENCIA) {
                add(out, c.prazoData(), "Exigência", c.nome(), c.id(),
                        "Cumprir exigência: " + nvl(c.exigenciaTexto(), "ver no site"), hoje, true);
            }
            if (c.etapa() == Etapa.INDEFERIDO) {
                String ciencia = c.cienciaData() != null ? " Ciência do cliente em " + Datas.br(c.cienciaData()) + "." : "";
                add(out, null, "Recurso", c.nome(), c.id(), "Indeferido: " + nvl(c.indeferimentoMotivo(), "motivo não informado")
                        + "." + ciencia + " Conferir o prazo recursal na decisão.", hoje, true);
            }
            if (a.rgpDias() != null && a.rgpDias() <= 60) {
                add(out, c.rgpValidade(), "RGP", c.nome(), c.id(), "Vencimento do RGP. Renovar para não travar o requerimento.", hoje, true);
            }
            if (a.procuracaoDias() != null && a.procuracaoDias() <= 90) {
                add(out, c.procuracaoValidade(), "Procuração", c.nome(), c.id(), "Vencimento da procuração e-CAC.", hoje, true);
            }
        }
        for (DefesoInfo d : defesos) {
            add(out, d.inicio(), "Defeso", nvl(d.regiao(), "Todas as colônias"), null, "Início do defeso de " + d.especie()
                    + (d.portaria() != null && !d.portaria().isBlank() ? " (Portaria " + d.portaria() + ")" : "") + ".", hoje, false);
            add(out, d.prazoRequerimento(), "Defeso", "Clientes aptos", null,
                    "Prazo final para requerer o seguro-defeso de " + d.especie() + ".", hoje, true);
        }
        if (defesos.isEmpty()) {
            out.add(new Prazo(null, "portaria", "Defeso", "Todas as colônias", null,
                    "Nenhum defeso cadastrado. Cadastre as datas da portaria em Defesos.", null, "cadastrar", "neutro"));
        }
        out.sort(Comparator.comparingLong(Prazos::ordem));
        return out;
    }

    private static long ordem(Prazo p) {
        if (p.dias() == null) {
            return "erro".equals(p.cor()) ? -99999 : 99999;
        }
        return p.dias();
    }

    private static void add(List<Prazo> out, LocalDate data, String tipo, String cliente, Long id, String texto,
                            LocalDate hoje, boolean urgenteSemData) {
        Long n = Datas.dias(data, hoje);
        String cor = (data != null || urgenteSemData) ? corPorDias(n) : "neutro";
        if (data != null && !urgenteSemData) {
            cor = n != null && n <= 15 ? "aviso" : "neutro";
        }
        out.add(new Prazo(data, data == null ? "conferir" : Datas.br(data), tipo, cliente, id, texto, n,
                data == null ? "conferir" : Datas.faltaTxt(n), cor));
    }

    static String nvl(String s, String padrao) {
        return s == null || s.isBlank() ? padrao : s;
    }
}
