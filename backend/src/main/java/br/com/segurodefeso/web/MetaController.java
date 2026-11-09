package br.com.segurodefeso.web;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.Mensagens;
import br.com.segurodefeso.dominio.Papel;
import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.dominio.StatusDae;
import br.com.segurodefeso.dominio.TipoDocumento;

/** Tabelas fixas (requisitos, documentos, etapas...) para o front-end montar as telas. */
@RestController
public class MetaController {

    public record Item(String id, String nome, String cor, String extra, boolean flag) {
    }

    @GetMapping("/api/meta")
    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("requisitos", Arrays.stream(Requisito.values())
            .map(r -> Map.of("id", r.name(), "texto", r.getTexto(), "curto", r.getCurto(), "prova", r.getProva(), "essencial", r.isEssencial()))
            .toList());
        m.put("baseLegal", Requisito.BASE_LEGAL);
        m.put("documentos", Arrays.stream(TipoDocumento.values())
            .map(t -> new Item(t.name(), t.getNome(), null, null, t.isObrigatorio())).toList());
        m.put("etapas", Arrays.stream(Etapa.values()).map(e -> new Item(e.name(), e.getNome(), e.getCor(), null, false)).toList());
        m.put("statusDae", Arrays.stream(StatusDae.values()).map(s -> new Item(s.name(), s.getNome(), s.getCor(), null, false)).toList());
        m.put("papeis", Arrays.stream(Papel.values()).map(p -> new Item(p.name(), p.getNome(), null, null, false)).toList());
        m.put("modelos", Arrays.stream(Mensagens.Modelo.values()).map(x -> new Item(x.name(), x.getNome(), null, null, false)).toList());
        m.put("sites", List.of(
            new Item("esocial", "eSocial", null, "https://login.esocial.gov.br/", false),
            new Item("emprega", "Emprega Brasil", null, "https://empregabrasil.mte.gov.br/", false),
            new Item("ecac", "e-CAC", null, "https://cav.receita.fazenda.gov.br/", false),
            new Item("meuinss", "Meu INSS", null, "https://meu.inss.gov.br/", false),
            new Item("govbr", "gov.br", null, "https://www.gov.br/", false)));
        return m;
    }
}
