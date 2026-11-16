package br.com.segurodefeso.dominio;

public final class Telefone {

    private Telefone() {
    }

    /** DDD + número: 10 dígitos (fixo) ou 11 (celular, começando com 9 depois do DDD). */
    public static boolean valido(String tel) {
        String d = tel == null ? "" : tel.replaceAll("\\D", "");
        if (d.length() != 10 && d.length() != 11) {
            return false;
        }
        if (Integer.parseInt(d.substring(0, 2)) < 11) {
            return false;
        }
        return d.length() == 10 || d.charAt(2) == '9';
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
