package br.com.segurodefeso.servico;

import static br.com.segurodefeso.dominio.Texto.limpar;
import static br.com.segurodefeso.dominio.Texto.vazio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Prazos;
import br.com.segurodefeso.entidade.Defeso;
import br.com.segurodefeso.repositorio.DefesoRepo;

/** Datas do defeso de cada espécie e região, conforme a portaria. */
@Service
public class DefesoService {

    public record Formulario(String especie, String regiao, String portaria, String inicio, String fim, String prazoRequerimento) {
    }

    private final DefesoRepo defesos;
    private final Hoje hoje;

    public DefesoService(DefesoRepo defesos, Hoje hoje) {
        this.defesos = defesos;
        this.hoje = hoje;
    }

    public List<Defeso> listar() {
        return defesos.findAllByOrderByInicioDesc();
    }

    /** Os defesos no formato que o cálculo de prazos usa. */
    public List<Prazos.DefesoInfo> infos() {
        return defesos.findAll().stream().map(d -> new Prazos.DefesoInfo(d.getEspecie(), d.getRegiao(), d.getPortaria(),
            d.getInicio(), d.getFim(), d.getPrazoRequerimento())).toList();
    }

    public Defeso criar(Formulario f) {
        LocalDate inicio = Datas.ler(f.inicio());
        LocalDate prazo = Datas.ler(f.prazoRequerimento());
        if (vazio(f.especie()) || inicio == null || prazo == null) {
            throw new RegraException("Informe a espécie, o início do defeso e o prazo final para requerer (datas em dd/mm/aaaa).");
        }
        Defeso d = new Defeso();
        d.setEspecie(f.especie().trim());
        d.setRegiao(limpar(f.regiao()));
        d.setPortaria(limpar(f.portaria()));
        d.setInicio(inicio);
        d.setFim(Datas.ler(f.fim()));
        d.setPrazoRequerimento(prazo);
        d.setCriadoEm(hoje.agora());
        return defesos.save(d);
    }

    public void excluir(Long id) {
        defesos.deleteById(id);
    }
}
