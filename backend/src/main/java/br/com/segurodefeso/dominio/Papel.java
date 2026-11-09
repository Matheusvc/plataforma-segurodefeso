package br.com.segurodefeso.dominio;

public enum Papel {
    ADMIN("Administrador"),
    ADVOGADO("Advogado"),
    EQUIPE("Equipe");

    private final String nome;

    Papel(String nome) {
        this.nome = nome;
    }

    public String getNome() { return nome; }
}
