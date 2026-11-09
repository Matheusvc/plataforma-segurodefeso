package br.com.segurodefeso.dominio;

import java.math.BigDecimal;

/** Valores configuráveis usados nos cálculos (tela Configurações). */
public record ConfigValores(
        BigDecimal salarioMinimo,
        BigDecimal honorariosPct,
        BigDecimal aliquotaDae,
        int diaVencimentoDae,
        int parcelasPadrao,
        String nomeEscritorio) {

    /** Padrões: salário mínimo de 2026 (Decreto 12.797/2025) e alíquota do segurado especial (1,2% + 0,1% + 0,2%). */
    public static ConfigValores padrao() {
        return new ConfigValores(new BigDecimal("1621.00"), new BigDecimal("30"), new BigDecimal("1.5"), 20, 4, "Escritório");
    }
}
