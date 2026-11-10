package br.com.segurodefeso.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.entidade.Auxiliar;
import br.com.segurodefeso.repositorio.AuxiliarRepo;
import br.com.segurodefeso.servico.NaoEncontradoException;
import br.com.segurodefeso.servico.RegraException;

/** Cadastro dos auxiliares jurídicos do escritório (escolhidos na ficha de cada cliente). */
@RestController
@RequestMapping("/api/auxiliares")
public class AuxiliarController {

    public record AuxiliarReq(String nome, String telefone, String oab, Boolean ativo) {
    }

    private final AuxiliarRepo repo;
    private final Hoje hoje;

    public AuxiliarController(AuxiliarRepo repo, Hoje hoje) {
        this.repo = repo;
        this.hoje = hoje;
    }

    @GetMapping
    public List<Auxiliar> listar() {
        return repo.findAllByOrderByNomeAsc();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> criar(@RequestBody AuxiliarReq req) {
        Auxiliar a = new Auxiliar();
        a.setCriadoEm(hoje.agora());
        preencher(a, req);
        repo.save(a);
        return Map.of("mensagem", "Auxiliar cadastrado.");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> atualizar(@PathVariable Long id, @RequestBody AuxiliarReq req) {
        Auxiliar a = repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Auxiliar não encontrado"));
        preencher(a, req);
        repo.save(a);
        return Map.of("mensagem", "Auxiliar atualizado.");
    }

    private static void preencher(Auxiliar a, AuxiliarReq req) {
        if (req.nome() == null || req.nome().isBlank()) {
            throw new RegraException("Informe o nome do auxiliar.");
        }
        a.setNome(req.nome().trim());
        a.setTelefone(vazioNulo(req.telefone()));
        a.setOab(vazioNulo(req.oab()));
        if (req.ativo() != null) {
            a.setAtivo(req.ativo());
        }
    }

    private static String vazioNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
