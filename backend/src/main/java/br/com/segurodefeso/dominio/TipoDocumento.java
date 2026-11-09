package br.com.segurodefeso.dominio;

import java.util.Arrays;
import java.util.List;

public enum TipoDocumento {
    RG("Identidade e CPF", true),
    RGP("Carteira ou extrato do RGP", true),
    RESIDENCIA("Comprovante de residência", true),
    PROCURACAO("Procuração e-CAC (eSocial)", true),
    CONTRATO("Contrato de honorários", true),
    LGPD("Termo de autorização e LGPD", true),
    VENDA("Comprovantes de comercialização", true),
    COMPROVANTE_DAE("Comprovante de pagamento do DAE", false),
    DECISAO("Decisão / exigência", false),
    OUTRO("Outro documento", false);

    private final String nome;
    private final boolean obrigatorio;

    TipoDocumento(String nome, boolean obrigatorio) {
        this.nome = nome;
        this.obrigatorio = obrigatorio;
    }

    public String getNome() { return nome; }
    public boolean isObrigatorio() { return obrigatorio; }

    public static List<TipoDocumento> obrigatorios() {
        return Arrays.stream(values()).filter(TipoDocumento::isObrigatorio).toList();
    }
}
