package br.com.segurodefeso.dominio;

/** Situação da entrevista do cliente. O DAE só é lançado para quem tem entrevista CONFIRMADA. */
public enum Entrevista {
    NAO("Não", "erro"),
    CONFIRMADO("Confirmado", "ok"),
    A_CONFIRMAR("A confirmar", "aviso");

    private final String nome;
    private final String cor;

    Entrevista(String nome, String cor) {
        this.nome = nome;
        this.cor = cor;
    }

    public String getNome() { return nome; }
    public String getCor() { return cor; }
}
