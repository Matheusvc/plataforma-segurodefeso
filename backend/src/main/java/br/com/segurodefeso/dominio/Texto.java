package br.com.segurodefeso.dominio;

/** Tratamento de texto digitado: vazio, espaços e valores padrão. */
public final class Texto {

    private Texto() {
    }

    public static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    /** Texto sem espaços nas pontas, ou null se estiver vazio. */
    public static String limpar(String s) {
        return vazio(s) ? null : s.trim();
    }

    public static String nvl(String s, String padrao) {
        return vazio(s) ? padrao : s;
    }
}
