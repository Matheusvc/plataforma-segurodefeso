package br.com.segurodefeso.web;

import java.util.List;

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

    @GetMapping
    public List<Prazos.Prazo> prazos(@RequestParam(defaultValue = "Todos") String tipo) {
        List<Prazos.Prazo> todos = Prazos.calcular(clientes.carregar(false), defesos.infos(), hoje.data());
        return tipo.equals("Todos") ? todos : todos.stream().filter(p -> p.tipo().equals(tipo)).toList();
    }
}
