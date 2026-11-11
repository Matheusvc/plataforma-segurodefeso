package br.com.segurodefeso.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.servico.InicioService;

@RestController
@RequestMapping("/api/inicio")
public class InicioController {

    private final InicioService inicio;

    public InicioController(InicioService inicio) {
        this.inicio = inicio;
    }

    @GetMapping
    public Map<String, Object> painel() {
        return inicio.painel();
    }
}
