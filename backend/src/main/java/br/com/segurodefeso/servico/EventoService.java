package br.com.segurodefeso.servico;

import org.springframework.stereotype.Service;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.entidade.Evento;
import br.com.segurodefeso.repositorio.EventoRepo;

/** Registro de atividades (linha do tempo do cliente e auditoria). */
@Service
public class EventoService {

    private final EventoRepo repo;
    private final Hoje hoje;

    public EventoService(EventoRepo repo, Hoje hoje) {
        this.repo = repo;
        this.hoje = hoje;
    }

    public void registrar(Long clienteId, Long usuarioId, String tipo, String descricao) {
        Evento e = new Evento();
        e.setClienteId(clienteId);
        e.setUsuarioId(usuarioId);
        e.setMomento(hoje.agora());
        e.setTipo(tipo);
        e.setDescricao(descricao.length() > 1000 ? descricao.substring(0, 1000) : descricao);
        repo.save(e);
    }
}
