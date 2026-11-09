package br.com.segurodefeso.web;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.dominio.Dinheiro;
import br.com.segurodefeso.dominio.Papel;
import br.com.segurodefeso.entidade.Cliente;
import br.com.segurodefeso.entidade.Usuario;
import br.com.segurodefeso.repositorio.ClienteRepo;
import br.com.segurodefeso.repositorio.EventoRepo;
import br.com.segurodefeso.repositorio.UsuarioRepo;
import br.com.segurodefeso.servico.ConfigService;
import br.com.segurodefeso.servico.EventoService;
import br.com.segurodefeso.servico.LoginService;
import br.com.segurodefeso.servico.NaoEncontradoException;
import br.com.segurodefeso.servico.RegraException;
import br.com.segurodefeso.servico.UsuarioAtual;

/** Usuários, configurações e registro de atividades. Acesso: só administrador (ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record NovoUsuario(String nome, String email, String senha, String papel) {
    }

    public record AlterarUsuario(String papel, Boolean ativo, String senha) {
    }

    public record ConfigReq(String nomeEscritorio, String salarioMinimo, String honorariosPct, String aliquotaDae,
                            Integer diaVencimentoDae, Integer parcelasPadrao) {
    }

    private final UsuarioRepo usuarios;
    private final ClienteRepo clientes;
    private final EventoRepo eventos;
    private final ConfigService config;
    private final EventoService eventoService;
    private final PasswordEncoder encoder;
    private final UsuarioAtual atual;
    private final Hoje hoje;

    public AdminController(UsuarioRepo usuarios, ClienteRepo clientes, EventoRepo eventos, ConfigService config,
                           EventoService eventoService, PasswordEncoder encoder, UsuarioAtual atual, Hoje hoje) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.eventos = eventos;
        this.config = config;
        this.eventoService = eventoService;
        this.encoder = encoder;
        this.atual = atual;
        this.hoje = hoje;
    }

    @GetMapping("/usuarios")
    public List<Map<String, Object>> usuarios() {
        return usuarios.findAllByOrderByNomeAsc().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("nome", u.getNome());
            m.put("email", u.getEmail());
            m.put("papel", u.getPapel());
            m.put("ativo", u.isAtivo());
            m.put("ultimoLogin", u.getUltimoLogin());
            return m;
        }).toList();
    }

    @PostMapping("/usuarios")
    public Map<String, String> criar(@RequestBody NovoUsuario req) {
        String nome = req.nome() == null ? "" : req.nome().trim();
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (nome.isEmpty() || !email.contains("@")) throw new RegraException("Informe nome e e-mail válidos.");
        if (!LoginService.senhaForte(req.senha())) throw new RegraException("Senha inicial fraca: use pelo menos 10 caracteres, com letras e números.");
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) throw new RegraException("Já existe usuário com este e-mail.");
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(req.senha()));
        u.setPapel(Papel.valueOf(req.papel()));
        u.setAtivo(true);
        u.setCriadoEm(hoje.agora());
        usuarios.save(u);
        return Map.of("mensagem", "Usuário " + email + " criado. Passe a senha inicial pessoalmente e peça para trocá-la no primeiro acesso.");
    }

    @PutMapping("/usuarios/{id}")
    public Map<String, String> alterar(@PathVariable Long id, @RequestBody AlterarUsuario req) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        boolean proprio = u.getId().equals(atual.id());
        if (req.papel() != null) {
            if (proprio) throw new RegraException("Você não pode mudar o próprio papel.");
            u.setPapel(Papel.valueOf(req.papel()));
        }
        if (req.ativo() != null) {
            if (proprio) throw new RegraException("Você não pode se desativar.");
            u.setAtivo(req.ativo());
        }
        if (req.senha() != null) {
            if (!LoginService.senhaForte(req.senha())) throw new RegraException("Senha fraca: use pelo menos 10 caracteres, com letras e números.");
            u.setSenhaHash(encoder.encode(req.senha()));
        }
        usuarios.save(u);
        return Map.of("mensagem", "Usuário atualizado.");
    }

    @GetMapping("/configuracoes")
    public ConfigValores configuracoes() {
        return config.valores();
    }

    @PutMapping("/configuracoes")
    public Map<String, Object> salvarConfig(@RequestBody ConfigReq req) {
        ConfigValores atualCfg = config.valores();
        BigDecimal sm = Dinheiro.ler(req.salarioMinimo());
        BigDecimal pct = Dinheiro.ler(req.honorariosPct());
        BigDecimal aliq = Dinheiro.ler(req.aliquotaDae());
        ConfigValores novo = config.salvar(new ConfigValores(sm, pct, aliq,
            req.diaVencimentoDae() == null ? atualCfg.diaVencimentoDae() : req.diaVencimentoDae(),
            req.parcelasPadrao() == null ? atualCfg.parcelasPadrao() : req.parcelasPadrao(),
            req.nomeEscritorio()));
        eventoService.registrar(null, atual.id(), "config", "Configurações alteradas");
        return Map.of("mensagem", "Configurações salvas. Os cálculos já usam os novos valores.", "config", novo);
    }

    @GetMapping("/atividades")
    public List<Map<String, Object>> atividades() {
        Map<Long, String> nomesU = new HashMap<>();
        usuarios.findAll().forEach(u -> nomesU.put(u.getId(), u.getNome()));
        Map<Long, String> nomesC = new HashMap<>();
        clientes.findAll().forEach((Cliente c) -> nomesC.put(c.getId(), c.getNome()));
        return eventos.findAllByOrderByMomentoDescIdDesc(PageRequest.of(0, 500)).stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("momento", e.getMomento());
            m.put("usuario", e.getUsuarioId() == null ? "sistema" : nomesU.get(e.getUsuarioId()));
            m.put("clienteId", e.getClienteId());
            m.put("cliente", e.getClienteId() == null ? null : nomesC.get(e.getClienteId()));
            m.put("tipo", e.getTipo());
            m.put("descricao", e.getDescricao());
            return m;
        }).toList();
    }
}
