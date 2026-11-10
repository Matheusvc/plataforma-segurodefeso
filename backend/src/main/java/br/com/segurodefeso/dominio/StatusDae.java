package br.com.segurodefeso.dominio;

/** Lançamento manual do DAE. Competência sem lançamento é considerada em aberto. */
public enum StatusDae {
    DECLARADO("Declarado", "azul"),
    PAGO("Pago", "ok");

    private final String nome;
    private final String cor;

    StatusDae(String nome, String cor) {
        this.nome = nome;
        this.cor = cor;
    }

    public String getNome() { return nome; }
    public String getCor() { return cor; }
}
