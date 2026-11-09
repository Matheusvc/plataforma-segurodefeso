package br.com.segurodefeso.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Dinheiro {

    private Dinheiro() {
    }

    /** "1.234,56" -> 1234.56 ; "1234.56" -> 1234.56 ; vazio ou inválido -> null. */
    public static BigDecimal ler(String texto) {
        if (texto == null) {
            return null;
        }
        String s = texto.replace("R$", "").trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.contains(",")) {
            s = s.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String brl(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        return "R$ " + f.format(v == null ? BigDecimal.ZERO : v);
    }

    public static BigDecimal pct(BigDecimal base, BigDecimal percentual) {
        return base.multiply(percentual).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
