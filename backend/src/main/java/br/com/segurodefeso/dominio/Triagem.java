package br.com.segurodefeso.dominio;

import java.util.List;
import java.util.Map;

/** Resultado da triagem feita no atendimento. */
public final class Triagem {

    public record Resultado(String cor, String titulo, String texto, List<String> itens) {
    }

    private Triagem() {
    }

    public static Resultado avaliar(Map<Requisito, Boolean> respostas) {
        List<String> naoEssenciais = new java.util.ArrayList<>();
        List<String> naoOutros = new java.util.ArrayList<>();
        int faltam = 0;
        for (Requisito r : Requisito.values()) {
            Boolean v = respostas.get(r);
            if (v == null) {
                faltam++;
            } else if (!v) {
                (r.isEssencial() ? naoEssenciais : naoOutros).add(r.getTexto());
            }
        }
        if (!naoEssenciais.isEmpty()) {
            return new Resultado("erro", "Não atende requisito essencial",
                    "Não fazer o pedido sem antes resolver ou comprovar o contrário:", naoEssenciais);
        }
        if (faltam > 0) {
            return new Resultado("info", "Triagem incompleta",
                    "Faltam " + faltam + " de " + Requisito.values().length + " respostas.", List.of());
        }
        if (!naoOutros.isEmpty()) {
            return new Resultado("aviso", "Apto, com pendências", "Regularizar antes de requerer:", naoOutros);
        }
        return new Resultado("ok", "Apto para requerer",
                "Todos os requisitos atendidos. Cadastre o cliente e confira os documentos.", List.of());
    }
}
