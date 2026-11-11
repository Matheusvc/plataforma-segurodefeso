package br.com.segurodefeso.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.servico.DaeService;

@RestController
@RequestMapping("/api/dae")
public class DaeController {

    private final DaeService dae;

    public DaeController(DaeService dae) {
        this.dae = dae;
    }

    @GetMapping
    public Map<String, Object> tela(@RequestParam(required = false) String competencia) {
        return dae.tela(competencia);
    }

    @PostMapping
    public Map<String, String> salvar(@RequestBody DaeService.Lancamento lancamento) {
        return Map.of("mensagem", dae.salvar(lancamento));
    }
}
