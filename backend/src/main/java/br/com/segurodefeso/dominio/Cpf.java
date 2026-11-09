package br.com.segurodefeso.dominio;

public final class Cpf {

    private Cpf() {
    }

    public static String digitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String c = digitos(cpf);
        if (c.length() != 11 || c.chars().distinct().count() == 1) {
            return false;
        }
        for (int n = 9; n <= 10; n++) {
            int soma = 0;
            for (int i = 0; i < n; i++) {
                soma += (c.charAt(i) - '0') * ((n + 1) - i);
            }
            int dig = (soma * 10) % 11 % 10;
            if (dig != c.charAt(n) - '0') {
                return false;
            }
        }
        return true;
    }

    public static String formatar(String cpf) {
        String c = digitos(cpf);
        if (c.length() != 11) {
            return cpf == null ? "" : cpf;
        }
        return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9);
    }

    public static String mascarar(String cpf) {
        String c = digitos(cpf);
        if (c.length() != 11) {
            return cpf == null ? "" : cpf;
        }
        return "***." + c.substring(3, 6) + "." + c.substring(6, 9) + "-**";
    }
}
