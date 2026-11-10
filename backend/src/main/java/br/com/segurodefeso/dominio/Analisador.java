package br.com.segurodefeso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Regras que analisam um cliente: requisitos, documentos, DAE, bloqueios e honorários. */
public final class Analisador {

    private Analisador() {
    }

    public static Analise analisar(ClienteDados c, ConfigValores cfg, LocalDate hoje) {
        // requisitos
        List<Analise.ItemRequisito> itens = new ArrayList<>();
        List<String> naoAtendidos = new ArrayList<>();
        int semResposta = 0;
        boolean essencialFalha = false;
        Map<Requisito, Boolean> reqs = c.requisitos() == null ? Map.of() : c.requisitos();
        for (Requisito r : Requisito.values()) {
            Boolean v = reqs.get(r);
            itens.add(new Analise.ItemRequisito(r, r.getTexto(), r.getProva(), r.isEssencial(), v));
            if (v == null) {
                semResposta++;
            } else if (!v) {
                naoAtendidos.add(r.getCurto());
                essencialFalha |= r.isEssencial();
            }
        }
        int ok = Requisito.values().length - naoAtendidos.size() - semResposta;

        // documentos
        Set<TipoDocumento> docs = c.documentos() == null ? Set.of() : c.documentos();
        List<TipoDocumento> pendentes = TipoDocumento.obrigatorios().stream().filter(t -> !docs.contains(t)).toList();
        Long rgpDias = Datas.dias(c.rgpValidade(), hoje);
        Long procDias = Datas.dias(c.procuracaoValidade(), hoje);
        boolean procValida = procDias != null && procDias >= 0;
        boolean rgpVencido = rgpDias != null && rgpDias < 0;

        // DAE
        // Só quem tem entrevista confirmada entra no DAE; competência sem lançamento (PAGO/DECLARADO) está em aberto.
        Map<String, ClienteDados.DaeInfo> daes = c.daes() == null ? Map.of() : c.daes();
        boolean noDae = c.entrevista() == Entrevista.CONFIRMADO;
        List<Analise.DaeAberto> vencidos = new ArrayList<>();
        List<Analise.DaeAberto> abertos = new ArrayList<>();
        List<Analise.DaeMes> ultimos = new ArrayList<>();
        if (noDae) {
            for (String comp : Datas.competenciasDae(hoje)) {
                ClienteDados.DaeInfo d = daes.get(comp);
                ultimos.add(new Analise.DaeMes(comp, Datas.competenciaLabel(comp), d == null ? null : d.status()));
                if (d == null) {
                    LocalDate venc = Datas.vencimentoDae(comp, cfg.diaVencimentoDae());
                    long n = Datas.dias(venc, hoje);
                    Analise.DaeAberto item = new Analise.DaeAberto(comp, Datas.competenciaLabel(comp), null, venc, n);
                    (n < 0 ? vencidos : abertos).add(item);
                }
            }
        }

        // bloqueios para requerer
        List<String> bloqueios = new ArrayList<>(naoAtendidos);
        if (semResposta > 0) {
            bloqueios.add(semResposta + " requisito(s) sem resposta na triagem");
        }
        if (!pendentes.isEmpty()) {
            bloqueios.add(pendentes.size() + " documento(s) pendente(s)");
        }
        if (!vencidos.isEmpty()) {
            bloqueios.add("DAE vencido: " + String.join(", ", vencidos.stream().map(Analise.DaeAberto::label).toList()));
        }
        String gov = c.govNivel() == null ? "" : c.govNivel().toLowerCase();
        if (!gov.equals("prata") && !gov.equals("ouro")) {
            bloqueios.add("conta gov.br sem nível prata/ouro");
        }
        if (rgpVencido) {
            bloqueios.add("RGP vencido");
        }

        // honorários
        BigDecimal pct = c.honorariosPct() != null ? c.honorariosPct() : cfg.honorariosPct();
        BigDecimal porParcela = Dinheiro.pct(cfg.salarioMinimo(), pct);
        int lib = c.parcelasLiberadas();
        int tot = c.parcelasTotal();
        BigDecimal recebido = c.honorariosRecebidos() == null ? BigDecimal.ZERO : c.honorariosRecebidos();
        BigDecimal devido = porParcela.multiply(BigDecimal.valueOf(lib));
        BigDecimal saldo = devido.subtract(recebido).max(BigDecimal.ZERO);
        BigDecimal futuro = porParcela.multiply(BigDecimal.valueOf(Math.max(0, tot - lib)));

        Etapa etapa = c.etapa() == null ? Etapa.DOCUMENTACAO : c.etapa();
        return new Analise(c, etapa.getNome(), etapa.getCor(), itens, naoAtendidos, ok, semResposta, essencialFalha,
                pendentes, rgpDias, procDias, procValida, rgpVencido, vencidos, abertos, ultimos, bloqueios,
                bloqueios.isEmpty(), pct, porParcela, devido, recebido, saldo, futuro);
    }
}
