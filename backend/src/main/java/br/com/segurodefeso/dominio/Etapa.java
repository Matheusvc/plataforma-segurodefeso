package br.com.segurodefeso.dominio;

public enum Etapa {
    DOCUMENTACAO("Documentação", "neutro"),
    A_REQUERER("A requerer", "info"),
    REQUERIDO("Requerido", "azul"),
    EXIGENCIA("Em exigência", "aviso"),
    DEFERIDO("Deferido", "ok"),
    INDEFERIDO("Indeferido", "erro");

    private final String nome;
    private final String cor;

    Etapa(String nome, String cor) {
        this.nome = nome;
        this.cor = cor;
    }

    public String getNome() { return nome; }
    public String getCor() { return cor; }
}
