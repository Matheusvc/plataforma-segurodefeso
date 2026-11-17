package br.com.segurodefeso.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Prazos;
import br.com.segurodefeso.servico.ClienteService;
import br.com.segurodefeso.servico.DefesoService;

@RestController
@RequestMapping("/api/prazos")
public class PrazoController {

    private final ClienteService clientes;
    private final DefesoService defesos;
    private final Hoje hoje;

    public PrazoController(ClienteService clientes, DefesoService defesos, Hoje hoje) {
        this.clientes = clientes;
        this.defesos = defesos;
        this.hoje = hoje;
    }

    /** Os prazos mais urgentes primeiro. itens traz até "limite" (máx. 1000); total é a quantidade completa. */
    @GetMapping
    public Map<String, Object> prazos(@RequestParam(defaultValue = "Todos") String tipo,
                                      @RequestParam(defaultValue = "300") int limite) {
        List<Prazos.Prazo> todos = Prazos.calcular(clientes.carregar(false), defesos.infos(), hoje.data());
        List<Prazos.Prazo> filtrados = tipo.equals("Todos") ? todos : todos.stream().filter(p -> p.tipo().equals(tipo)).toList();
        int max = Math.max(1, Math.min(limite, 1000));
        return Map.of("itens", filtrados.size() > max ? filtrados.subList(0, max) : filtrados, "total", filtrados.size());
    }
}
