package br.com.segurodefeso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cliente já analisado: o que falta, o que está vencido e quanto há de honorários. */
public record Analise(
        ClienteDados cliente,
        String etapaNome,
        String etapaCor,
        List<ItemRequisito> requisitos,
        List<String> requisitosNaoAtendidos,
        int requisitosOk,
        int requisitosSemResposta,
        boolean essencialFalha,
        List<TipoDocumento> documentosPendentes,
        Long rgpDias,
        Long procuracaoDias,
        boolean procuracaoValida,
        boolean rgpVencido,
        List<DaeAberto> daeVencidos,
        List<DaeAberto> daeAbertos,
        List<DaeMes> daeUltimosMeses,
        List<String> bloqueios,
        boolean apto,
        BigDecimal honorariosPct,
        BigDecimal honorariosPorParcela,
        BigDecimal honorariosDevidos,
        BigDecimal honorariosRecebidos,
        BigDecimal honorariosSaldo,
        BigDecimal honorariosFuturos) {

    public record ItemRequisito(Requisito requisito, String texto, String prova, boolean essencial, Boolean atende) {
    }

    public record DaeAberto(String competencia, String label, StatusDae status, LocalDate vencimento, long dias) {
    }

    public record DaeMes(String competencia, String label, StatusDae status) {
    }

    public Long id() {
        return cliente.id();
    }

    public String nome() {
        return cliente.nome();
    }
}
