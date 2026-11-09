package br.com.segurodefeso.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.entidade.Usuario;
import br.com.segurodefeso.repositorio.UsuarioRepo;
import br.com.segurodefeso.servico.LoginService;
import br.com.segurodefeso.servico.RegraException;
import br.com.segurodefeso.servico.UsuarioAtual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginReq(String email, String senha) {
    }

    public record SenhaReq(String atual, String nova, String confirma) {
    }

    public record Eu(Long id, String nome, String email, String papel, String papelNome, String hoje) {
    }

    private final AuthenticationManager auth;
    private final SecurityContextRepository contexto;
    private final LoginService login;
    private final UsuarioRepo usuarios;
    private final UsuarioAtual atual;
    private final PasswordEncoder encoder;
    private final Hoje hoje;

    public AuthController(AuthenticationManager auth, SecurityContextRepository contexto, LoginService login,
                          UsuarioRepo usuarios, UsuarioAtual atual, PasswordEncoder encoder, Hoje hoje) {
        this.auth = auth;
        this.contexto = contexto;
        this.login = login;
        this.usuarios = usuarios;
        this.atual = atual;
        this.encoder = encoder;
        this.hoje = hoje;
    }

    /** Chamado pelo React ao abrir: faz o servidor enviar o cookie XSRF-TOKEN. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("cabecalho", token.getHeaderName());
    }

    @PostMapping("/login")
    public ResponseEntity<?> entrar(@RequestBody LoginReq req, HttpServletRequest request, HttpServletResponse response) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (login.bloqueado(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("erro",
                "Muitas tentativas. Aguarde " + LoginService.BLOQUEIO_MINUTOS + " minutos e tente de novo."));
        }
        Authentication a;
        try {
            a = auth.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, req.senha() == null ? "" : req.senha()));
        } catch (AuthenticationException e) {
            login.registrar(email, false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "E-mail ou senha incorretos."));
        }
        login.registrar(email, true);
        if (request.getSession(false) != null) {
            request.changeSessionId();   // evita fixação de sessão
        }
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(a);
        SecurityContextHolder.setContext(ctx);
        contexto.saveContext(ctx, request, response);
        Usuario u = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        u.setUltimoLogin(hoje.agora());
        usuarios.save(u);
        return ResponseEntity.ok(eu(u));
    }

    @GetMapping("/eu")
    public Eu eu() {
        return eu(atual.get());
    }

    @PostMapping("/senha")
    public Map<String, String> senha(@RequestBody SenhaReq req) {
        Usuario u = atual.get();
        if (req.atual() == null || !encoder.matches(req.atual(), u.getSenhaHash())) {
            throw new RegraException("A senha atual não confere.");
        }
        if (!LoginService.senhaForte(req.nova())) {
            throw new RegraException("A nova senha precisa ter pelo menos 10 caracteres, com letras e números.");
        }
        if (!req.nova().equals(req.confirma())) {
            throw new RegraException("A confirmação não confere com a nova senha.");
        }
        u.setSenhaHash(encoder.encode(req.nova()));
        usuarios.save(u);
        return Map.of("mensagem", "Senha alterada.");
    }

    private Eu eu(Usuario u) {
        return new Eu(u.getId(), u.getNome(), u.getEmail(), u.getPapel().name(), u.getPapel().getNome(), hoje.data().toString());
    }
}
