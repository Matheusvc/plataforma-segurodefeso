package br.com.segurodefeso.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.dominio.StatusDae;
import br.com.segurodefeso.dominio.TipoDocumento;
import br.com.segurodefeso.entidade.Cliente;
import br.com.segurodefeso.entidade.Dae;
import br.com.segurodefeso.entidade.Documento;
import br.com.segurodefeso.entidade.RequisitoCliente;
import br.com.segurodefeso.repositorio.ClienteRepo;
import br.com.segurodefeso.repositorio.DaeRepo;
import br.com.segurodefeso.repositorio.DocumentoRepo;
import br.com.segurodefeso.repositorio.RequisitoRepo;

/** 10 clientes fictícios para treinar a equipe (ligado por DADOS_EXEMPLO=true; nunca em produção). */
@Service
public class ExemploService {

    private record Ex(String nome, String municipio, String colonia, Etapa etapa, String gov, Integer procDias, int rgpDias,
                      String requisitos, boolean docsOk, String dae, int parcelasLib, String extra) {
    }

    private static final List<Ex> EXEMPLOS = List.of(
        new Ex("Maria da Conceição Sousa", "Raposa", "Colônia Z-53", Etapa.DEFERIDO, "prata", 120, 400, "1111111", true, "pppppg", 2, null),
        new Ex("José Ribamar Costa", "São José de Ribamar", "Colônia Z-10", Etapa.EXIGENCIA, "prata", 23, 300, "1111111", true, "ppppp-", 0, "comprovante de residência atualizado"),
        new Ex("Antônia Ferreira Lima", "Cururupu", "Colônia Z-08", Etapa.A_REQUERER, "bronze", 180, 21, "1111010", false, "pppp--", 0, null),
        new Ex("Francisco das Chagas Pereira", "Tutóia", "Colônia Z-29", Etapa.REQUERIDO, "ouro", 280, 500, "1111111", true, "pppppe", 0, null),
        new Ex("Raimunda Nonata Alves", "Carutapera", "Colônia Z-17", Etapa.A_REQUERER, "prata", null, 365, "1111011", false, "ppp---", 0, null),
        new Ex("Pedro Henrique Mendes", "Humberto de Campos", "Colônia Z-21", Etapa.INDEFERIDO, "prata", 53, 400, "1101111", true, "pppppg", 0, "vínculo de emprego no CNIS"),
        new Ex("Benedita Santos Rocha", "Raposa", "Colônia Z-53", Etapa.DEFERIDO, "ouro", 220, 600, "1111111", true, "pppppp", 4, null),
        new Ex("Manoel Silva Nascimento", "Cururupu", "Colônia Z-08", Etapa.DOCUMENTACAO, "bronze", null, 700, "1111000", false, "", 0, null),
        new Ex("Luzia Correia Pinto", "Tutóia", "Colônia Z-29", Etapa.A_REQUERER, "prata", 170, 500, "1111111", true, "pppppg", 0, null),
        new Ex("Sebastião Ramos Filho", "Raposa", "Colônia Z-53", Etapa.DEFERIDO, "prata", 100, 450, "1111111", true, "pppppg", 3, null));

    private final ClienteRepo clientes;
    private final RequisitoRepo requisitos;
    private final DocumentoRepo documentos;
    private final DaeRepo daes;
    private final ConfigService config;
    private final EventoService eventos;
    private final Hoje hoje;

    public ExemploService(ClienteRepo clientes, RequisitoRepo requisitos, DocumentoRepo documentos, DaeRepo daes,
                          ConfigService config, EventoService eventos, Hoje hoje) {
        this.clientes = clientes;
        this.requisitos = requisitos;
        this.documentos = documentos;
        this.daes = daes;
        this.config = config;
        this.eventos = eventos;
        this.hoje = hoje;
    }

    static String cpfFicticio(long semente) {
        Random rnd = new Random(semente);
        int[] n = new int[11];
        for (int i = 0; i < 9; i++) n[i] = rnd.nextInt(10);
        for (int k = 9; k <= 10; k++) {
            int s = 0;
            for (int i = 0; i < k; i++) s += n[i] * ((k + 1) - i);
            n[k] = (s * 10) % 11 % 10;
        }
        StringBuilder sb = new StringBuilder();
        for (int d : n) sb.append(d);
        return sb.toString();
    }

    @Transactional
    public int carregar() {
        ConfigValores cfg = config.valores();
        LocalDate h = hoje.data();
        List<String> comps = Datas.competenciasDae(h);
        int criados = 0;
        for (int i = 0; i < EXEMPLOS.size(); i++) {
            Ex e = EXEMPLOS.get(i);
            String cpf = cpfFicticio(1000 + i);
            if (clientes.findByCpf(cpf).isPresent()) continue;
            Cliente c = new Cliente();
            c.setNome(e.nome());
            c.setCpf(cpf);
            c.setTelefone(String.format("(98) 9%04d-%04d", 8000 + i, 1000 + i * 7));
            c.setMunicipio(e.municipio());
            c.setColonia(e.colonia());
            c.setRgpNumero(String.format("MA-%06d", 100 + i));
            c.setRgpDesde("05/2019");
            c.setRgpValidade(h.plusDays(e.rgpDias()));
            c.setGovNivel(e.gov());
            c.setProcuracaoValidade(e.procDias() == null ? null : h.plusDays(e.procDias()));
            c.setEtapa(e.etapa());
            if (e.etapa() != Etapa.DOCUMENTACAO && e.etapa() != Etapa.A_REQUERER) {
                c.setProtocolo(String.format("2026/%06d", 200 + i));
            }
            if (e.etapa() == Etapa.EXIGENCIA) {
                c.setExigenciaTexto(e.extra());
                c.setPrazoData(h.plusDays(10));
            }
            if (e.etapa() == Etapa.INDEFERIDO) {
                c.setIndeferimentoMotivo(e.extra());
            }
            c.setParcelasTotal(cfg.parcelasPadrao());
            c.setParcelasLiberadas(e.parcelasLib());
            c.setSituacao("Dados de exemplo");
            c.setAtivo(true);
            c.setCriadoEm(hoje.agora());
            c.setAtualizadoEm(hoje.agora());
            c = clientes.save(c);
            Requisito[] rs = Requisito.values();
            for (int k = 0; k < rs.length; k++) {
                RequisitoCliente rc = new RequisitoCliente();
                rc.setClienteId(c.getId());
                rc.setChave(rs[k]);
                rc.setAtende(e.requisitos().charAt(k) == '1');
                requisitos.save(rc);
            }
            List<TipoDocumento> tipos = e.docsOk() ? TipoDocumento.obrigatorios() : List.of(TipoDocumento.RG, TipoDocumento.RGP);
            for (TipoDocumento t : tipos) {
                Documento d = new Documento();
                d.setClienteId(c.getId());
                d.setTipo(t);
                d.setNomeOriginal("exemplo_" + t.name().toLowerCase() + ".pdf");
                d.setArquivo("inexistente.pdf");
                d.setTamanho(0);
                d.setEnviadoEm(hoje.agora());
                documentos.save(d);
            }
            c.setEntrevista(e.dae().isEmpty() ? Entrevista.A_CONFIRMAR : Entrevista.CONFIRMADO);
            clientes.save(c);
            for (int k = 0; k < e.dae().length() && k < comps.size(); k++) {
                char s = e.dae().charAt(k);
                StatusDae st = switch (s) {
                    case 'p' -> StatusDae.PAGO;
                    case 'g', 'e' -> StatusDae.DECLARADO;
                    default -> null;
                };
                if (st == null) continue;
                BigDecimal venda = BigDecimal.valueOf(800 + i * 70L).setScale(2, RoundingMode.HALF_UP);
                Dae d = new Dae();
                d.setClienteId(c.getId());
                d.setCompetencia(comps.get(k));
                d.setValorVenda(venda);
                d.setValorDae(venda.multiply(cfg.aliquotaDae()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                d.setStatus(st);
                d.setAtualizadoEm(hoje.agora());
                daes.save(d);
            }
            eventos.registrar(c.getId(), null, "cadastro", "Cliente de exemplo criado");
            criados++;
        }
        return criados;
    }
}
