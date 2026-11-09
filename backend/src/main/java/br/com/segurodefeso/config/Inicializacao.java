package br.com.segurodefeso.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.segurodefeso.dominio.Papel;
import br.com.segurodefeso.entidade.Usuario;
import br.com.segurodefeso.repositorio.UsuarioRepo;
import br.com.segurodefeso.servico.ExemploService;
import br.com.segurodefeso.servico.LoginService;

/** Na primeira execução: cria o administrador (ADMIN_EMAIL/ADMIN_SENHA) e, se pedido, os dados de exemplo. */
@Component
public class Inicializacao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Inicializacao.class);

    private final UsuarioRepo usuarios;
    private final PasswordEncoder encoder;
    private final ExemploService exemplo;
    private final Hoje hoje;
    private final String adminNome;
    private final String adminEmail;
    private final String adminSenha;
    private final boolean dadosExemplo;

    public Inicializacao(UsuarioRepo usuarios, PasswordEncoder encoder, ExemploService exemplo, Hoje hoje,
                         @Value("${app.admin-nome:Administrador}") String adminNome,
                         @Value("${app.admin-email:}") String adminEmail,
                         @Value("${app.admin-senha:}") String adminSenha,
                         @Value("${app.dados-exemplo:false}") boolean dadosExemplo) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.exemplo = exemplo;
        this.hoje = hoje;
        this.adminNome = adminNome;
        this.adminEmail = adminEmail;
        this.adminSenha = adminSenha;
        this.dadosExemplo = dadosExemplo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() == 0) {
            if (adminEmail.isBlank() || !LoginService.senhaForte(adminSenha)) {
                log.warn("Nenhum usuário cadastrado. Defina ADMIN_EMAIL e ADMIN_SENHA (mín. 10 caracteres, letras e números) "
                    + "e reinicie para criar o administrador.");
            } else {
                Usuario u = new Usuario();
                u.setNome(adminNome);
                u.setEmail(adminEmail.trim().toLowerCase());
                u.setSenhaHash(encoder.encode(adminSenha));
                u.setPapel(Papel.ADMIN);
                u.setAtivo(true);
                u.setCriadoEm(hoje.agora());
                usuarios.save(u);
                log.info("Administrador {} criado. Remova ADMIN_SENHA do ambiente depois do primeiro acesso.", u.getEmail());
            }
        }
        if (dadosExemplo) {
            int n = exemplo.carregar();
            log.warn("{} clientes de exemplo carregados (DADOS_EXEMPLO=true). Não use em produção.", n);
        }
    }
}
