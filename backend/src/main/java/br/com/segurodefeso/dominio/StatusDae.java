package br.com.segurodefeso.dominio;

public enum StatusDae {
    PENDENTE("Em aberto", "erro"),
    GERADO("Gerado", "azul"),
    ENVIADO("Enviado", "aviso"),
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
