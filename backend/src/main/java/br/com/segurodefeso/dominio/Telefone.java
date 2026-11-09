package br.com.segurodefeso.dominio;

public final class Telefone {

    private Telefone() {
    }

    /** Converte o telefone para o formato do wa.me (55 + DDD + número). Null se não der. */
    public static String whatsapp(String tel) {
        String d = tel == null ? "" : tel.replaceAll("\\D", "");
        if (d.isEmpty()) {
            return null;
        }
        if (d.startsWith("55") && d.length() >= 12) {
            return d;
        }
        if (d.length() == 10 || d.length() == 11) {
            return "55" + d;
        }
        return null;
    }
}
