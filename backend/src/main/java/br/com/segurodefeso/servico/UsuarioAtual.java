package br.com.segurodefeso.servico;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.com.segurodefeso.dominio.Papel;
import br.com.segurodefeso.entidade.Usuario;
import br.com.segurodefeso.repositorio.UsuarioRepo;

@Component
public class UsuarioAtual {

    private final UsuarioRepo usuarios;

    public UsuarioAtual(UsuarioRepo usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new NaoEncontradoException("Usuário não autenticado");
        }
        return usuarios.findByEmailIgnoreCase(a.getName())
            .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
    }

    public Long id() {
        return get().getId();
    }

    public boolean gestor() {
        Papel p = get().getPapel();
        return p == Papel.ADMIN || p == Papel.ADVOGADO;
    }
}
