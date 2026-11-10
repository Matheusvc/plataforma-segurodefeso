package br.com.segurodefeso.dominio;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Datas {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BR_FLEX = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final String[] MESES = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};

    private Datas() {
    }

    /** Aceita "dd/mm/aaaa", "d/m/aaaa" ou "aaaa-mm-dd". Devolve null se vazio ou inválido. */
    public static LocalDate ler(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim();
        for (DateTimeFormatter f : new DateTimeFormatter[] {BR, BR_FLEX, DateTimeFormatter.ISO_LOCAL_DATE}) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // tenta o próximo formato
            }
        }
        return null;
    }

    public static String br(LocalDate d) {
        return d == null ? "" : d.format(BR);
    }

    public static Long dias(LocalDate d, LocalDate hoje) {
        return d == null ? null : ChronoUnit.DAYS.between(hoje, d);
    }

    public static String faltaTxt(Long n) {
        if (n == null) {
            return "conferir";
        }
        if (n < 0) {
            return "vencido há " + (-n) + " dia(s)";
        }
        if (n == 0) {
            return "hoje";
        }
        return "em " + n + " dia(s)";
    }

    /** "2026-11" -> "nov/26" */
    public static String competenciaLabel(String comp) {
        YearMonth ym = YearMonth.parse(comp);
        return MESES[ym.getMonthValue() - 1] + "/" + String.valueOf(ym.getYear()).substring(2);
    }

    /** As n competências (AAAA-MM) terminando no mês de ref, da mais antiga para a mais recente. */
    public static List<String> competenciasAte(LocalDate ref, int n) {
        List<String> out = new ArrayList<>();
        YearMonth ym = YearMonth.from(ref);
        for (int i = 0; i < n; i++) {
            out.add(ym.toString());
            ym = ym.minusMonths(1);
        }
        Collections.reverse(out);
        return out;
    }

    /** Primeira competência controlada no DAE (maio de 2026). */
    public static final String DAE_INICIO = "2026-05";

    /** Competências de DAE: de maio até o mês de ref, da mais antiga para a mais recente (mínimo: o mês de ref). */
    public static List<String> competenciasDae(LocalDate ref) {
        YearMonth fim = YearMonth.from(ref);
        YearMonth ym = YearMonth.parse(DAE_INICIO);
        List<String> out = new ArrayList<>();
        while (!ym.isAfter(fim)) {
            out.add(ym.toString());
            ym = ym.plusMonths(1);
        }
        if (out.isEmpty()) {
            out.add(fim.toString());
        }
        return out;
    }

    /** Vencimento estimado do DAE: dia X do mês seguinte à competência (o oficial é o da guia). */
    public static LocalDate vencimentoDae(String comp, int dia) {
        YearMonth seguinte = YearMonth.parse(comp).plusMonths(1);
        return seguinte.atDay(Math.max(1, Math.min(dia, 28)));
    }
}
