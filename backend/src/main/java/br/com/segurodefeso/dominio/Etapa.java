package br.com.segurodefeso.dominio;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

    /** Quantos clientes há em cada etapa (todas as etapas aparecem, mesmo com zero). */
    public static Map<Etapa, Long> contar(List<Analise> clientes) {
        Map<Etapa, Long> cont = new EnumMap<>(Etapa.class);
        for (Etapa e : values()) {
            cont.put(e, 0L);
        }
        clientes.forEach(a -> cont.merge(a.cliente().etapa(), 1L, Long::sum));
        return cont;
    }
}
