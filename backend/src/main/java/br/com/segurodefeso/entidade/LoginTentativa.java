package br.com.segurodefeso.entidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.segurodefeso.dominio.*;
import jakarta.persistence.*;

@Entity
@Table(name = "login_tentativas")
public class LoginTentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private LocalDateTime momento;
    private boolean sucesso;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getMomento() { return momento; }
    public void setMomento(LocalDateTime momento) { this.momento = momento; }
    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
}
