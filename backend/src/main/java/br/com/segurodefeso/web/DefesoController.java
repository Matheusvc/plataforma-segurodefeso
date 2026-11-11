package br.com.segurodefeso.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.entidade.Defeso;
import br.com.segurodefeso.servico.DefesoService;

@RestController
@RequestMapping("/api/defesos")
public class DefesoController {

    private final DefesoService defesos;

    public DefesoController(DefesoService defesos) {
        this.defesos = defesos;
    }

    @GetMapping
    public List<Defeso> listar() {
        return defesos.listar();
    }

    @PostMapping
    public Map<String, String> criar(@RequestBody DefesoService.Formulario form) {
        Defeso d = defesos.criar(form);
        return Map.of("mensagem", "Defeso de " + d.getEspecie() + " cadastrado. Os prazos já aparecem na tela Prazos.");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> excluir(@PathVariable Long id) {
        defesos.excluir(id);
        return Map.of("mensagem", "Defeso excluído.");
    }
}
