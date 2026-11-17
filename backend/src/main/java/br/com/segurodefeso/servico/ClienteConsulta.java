package br.com.segurodefeso.servico;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.com.segurodefeso.dominio.Cpf;
import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.entidade.Cliente;
import jakarta.persistence.criteria.Predicate;

/** Filtros de lista de clientes feitos no banco (busca, etapa, situação do cadastro, CPF e entrevista). */
public final class ClienteConsulta {

    public static final int TAMANHO_PADRAO = 50;
    public static final int TAMANHO_MAXIMO = 200;

    private ClienteConsulta() {
    }

    public static int tamanho(Integer pedido) {
        return pedido == null ? TAMANHO_PADRAO : Math.max(1, Math.min(pedido, TAMANHO_MAXIMO));
    }

    public static int pagina(Integer pedida) {
        return pedida == null ? 0 : Math.max(0, pedida);
    }

    /** Texto digitado vira padrão LIKE seguro: sem os curingas % e _. */
    private static String padrao(String texto) {
        return "%" + texto.replace("\\", "").replace("%", "").replace("_", "") + "%";
    }

    public static Specification<Cliente> busca(String q, boolean arquivados) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (!arquivados) {
                p.add(cb.isTrue(root.get("ativo")));
            }
            String busca = q == null ? "" : q.trim().toLowerCase();
            if (!busca.isEmpty()) {
                List<Predicate> ou = new ArrayList<>();
                String like = padrao(busca);
                ou.add(cb.like(cb.lower(root.get("nome")), like));
                ou.add(cb.like(cb.lower(root.get("municipio")), like));
                ou.add(cb.like(cb.lower(root.get("colonia")), like));
                String digitos = Cpf.digitos(busca);
                if (!digitos.isEmpty()) {
                    ou.add(cb.like(root.get("cpf"), "%" + digitos + "%"));
                }
                p.add(cb.or(ou.toArray(Predicate[]::new)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /**
     * Filtro da lista: todos, andamento (cadastro a finalizar), concluido, cpf (não conferido),
     * pendencia (documentação ou a requerer; o "não apto" é refinado depois), parcelas (deferidos com parcelas a liberar)
     * ou o nome de uma etapa.
     */
    public static Specification<Cliente> filtro(String filtro) {
        String f = filtro == null ? "todos" : filtro.trim();
        return (root, query, cb) -> switch (f) {
            case "todos" -> cb.conjunction();
            case "andamento" -> cb.isFalse(root.get("cadastroConcluido"));
            case "concluido" -> cb.isTrue(root.get("cadastroConcluido"));
            case "cpf" -> cb.isNull(root.get("cpfConferidoEm"));
            case "pendencia" -> root.get("etapa").in(Etapa.DOCUMENTACAO, Etapa.A_REQUERER);
            case "parcelas" -> cb.and(cb.equal(root.get("etapa"), Etapa.DEFERIDO),
                cb.lessThan(root.<Integer>get("parcelasLiberadas"), root.<Integer>get("parcelasTotal")));
            default -> {
                try {
                    yield cb.equal(root.get("etapa"), Etapa.valueOf(f.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    yield cb.disjunction();
                }
            }
        };
    }

    public static Specification<Cliente> etapas(List<Etapa> etapas) {
        return (root, query, cb) -> etapas == null || etapas.isEmpty() ? cb.conjunction() : root.get("etapa").in(etapas);
    }

    public static Specification<Cliente> entrevista(Entrevista entrevista) {
        return (root, query, cb) -> entrevista == null ? cb.conjunction() : cb.equal(root.get("entrevista"), entrevista);
    }
}
