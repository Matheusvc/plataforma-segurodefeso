package br.com.segurodefeso.servico;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.entidade.LoginTentativa;
import br.com.segurodefeso.repositorio.LoginTentativaRepo;

/** Bloqueia o login por 15 minutos depois de 5 tentativas erradas. */
@Service
public class LoginService {

    public static final int MAX_TENTATIVAS = 5;
    public static final int BLOQUEIO_MINUTOS = 15;

    private final LoginTentativaRepo repo;
    private final Hoje hoje;

    public LoginService(LoginTentativaRepo repo, Hoje hoje) {
        this.repo = repo;
        this.hoje = hoje;
    }

    @Transactional(readOnly = true)
    public boolean bloqueado(String email) {
        return repo.countByEmailAndSucessoFalseAndMomentoAfter(email, hoje.agora().minusMinutes(BLOQUEIO_MINUTOS)) >= MAX_TENTATIVAS;
    }

    @Transactional
    public void registrar(String email, boolean sucesso) {
        LoginTentativa t = new LoginTentativa();
        t.setEmail(email);
        t.setMomento(hoje.agora());
        t.setSucesso(sucesso);
        repo.save(t);
    }

    public static boolean senhaForte(String senha) {
        return senha != null && senha.length() >= 10 && senha.chars().anyMatch(Character::isDigit)
            && senha.chars().anyMatch(Character::isLetter);
    }
}
