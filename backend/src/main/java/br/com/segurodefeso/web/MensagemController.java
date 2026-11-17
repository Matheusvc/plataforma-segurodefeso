package br.com.segurodefeso.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.Mensagens;
import br.com.segurodefeso.servico.ClienteService;
import br.com.segurodefeso.servico.ConfigService;

/** Texto pronto de WhatsApp para o cliente. */
@RestController
@RequestMapping("/api/mensagens")
public class MensagemController {

    private final ClienteService clientes;
    private final ConfigService config;

    public MensagemController(ClienteService clientes, ConfigService config) {
        this.clientes = clientes;
        this.config = config;
    }

    @GetMapping
    public Map<String, Object> mensagem(@RequestParam Long cliente, @RequestParam(defaultValue = "DAE") String modelo) {
        Analise a = clientes.carregar(cliente);
        String texto = Mensagens.texto(Mensagens.Modelo.valueOf(modelo), a, config.valores());
        Map<String, Object> m = new HashMap<>();
        m.put("nome", a.cliente().nome());
        m.put("texto", texto);
        m.put("link", Mensagens.linkWhatsapp(a.cliente().telefone(), texto));
        m.put("telefone", a.cliente().telefone());
        return m;
    }
}
