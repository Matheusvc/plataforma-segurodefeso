package br.com.segurodefeso.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.servico.RelatorioService;

/** Honorários e relatórios: o acesso é restrito a administrador e advogado em SecurityConfig. */
@RestController
@RequestMapping("/api")
public class FinanceiroController {

    private final RelatorioService relatorios;

    public FinanceiroController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/honorarios")
    public Map<String, Object> honorarios() {
        return relatorios.honorarios();
    }

    @GetMapping("/relatorios")
    public Map<String, Object> relatorios() {
        return relatorios.relatorios();
    }
}
