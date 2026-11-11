package br.com.segurodefeso.web;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.dominio.Triagem;

/** A triagem só avalia: o cliente é criado depois, junto com o cadastro completo. */
@RestController
@RequestMapping("/api/triagem")
public class TriagemController {

    public record Pedido(Map<String, Boolean> respostas) {
    }

    @PostMapping("/avaliar")
    public Triagem.Resultado avaliar(@RequestBody Pedido pedido) {
        return Triagem.avaliar(Requisito.respostas(pedido.respostas()));
    }
}
