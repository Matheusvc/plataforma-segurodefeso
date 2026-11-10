package br.com.segurodefeso.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.ClienteDados;
import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.dominio.Cpf;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Entrevista;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.dominio.Telefone;
import br.com.segurodefeso.dominio.TipoDocumento;
import br.com.segurodefeso.entidade.Cliente;
import br.com.segurodefeso.entidade.Documento;
import br.com.segurodefeso.repositorio.DocumentoRepo;
import br.com.segurodefeso.repositorio.EventoRepo;
import br.com.segurodefeso.repositorio.HonorarioRepo;
import br.com.segurodefeso.repositorio.UsuarioRepo;
import br.com.segurodefeso.servico.ArquivoService;
import br.com.segurodefeso.servico.ClienteService;
import br.com.segurodefeso.servico.ConfigService;
import br.com.segurodefeso.servico.NaoEncontradoException;
import br.com.segurodefeso.servico.RegraException;
import br.com.segurodefeso.servico.UsuarioAtual;

@RestController
@RequestMapping("/api")
public class ClienteController {

    public record HonorarioReq(String valor, String data, String observacao) {
    }

    private final ClienteService servico;
    private final DocumentoRepo documentos;
    private final EventoRepo eventos;
    private final HonorarioRepo honorarios;
    private final UsuarioRepo usuarios;
    private final ArquivoService arquivos;
    private final ConfigService config;
    private final UsuarioAtual atual;
    private final Hoje hoje;

    public ClienteController(ClienteService servico, DocumentoRepo documentos, EventoRepo eventos, HonorarioRepo honorarios,
                             UsuarioRepo usuarios, ArquivoService arquivos, ConfigService config, UsuarioAtual atual, Hoje hoje) {
        this.servico = servico;
        this.documentos = documentos;
        this.eventos = eventos;
        this.honorarios = honorarios;
        this.usuarios = usuarios;
        this.arquivos = arquivos;
        this.config = config;
        this.atual = atual;
        this.hoje = hoje;
    }

    /** Lista com filtros: q (nome, CPF, município, colônia), filtro (todos, pendencia ou uma etapa), arquivados. */
    @GetMapping("/clientes")
    public List<Analise> listar(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "todos") String filtro,
                                @RequestParam(defaultValue = "false") boolean arquivados) {
        String busca = q.trim().toLowerCase();
        String dig = Cpf.digitos(busca);
        return servico.carregar(arquivados).stream().filter(a -> {
            ClienteDados c = a.cliente();
            if (!busca.isEmpty()) {
                boolean achou = (c.nome() != null && c.nome().toLowerCase().contains(busca))
                    || (!dig.isEmpty() && c.cpf().contains(dig))
                    || (c.municipio() != null && c.municipio().toLowerCase().contains(busca))
                    || (c.colonia() != null && c.colonia().toLowerCase().contains(busca));
                if (!achou) return false;
            }
            if (filtro.equals("todos")) return true;
            if (filtro.equals("pendencia")) {
                return !a.apto() && (c.etapa() == Etapa.DOCUMENTACAO || c.etapa() == Etapa.A_REQUERER);
            }
            return c.etapa().name().equalsIgnoreCase(filtro);
        }).toList();
    }

    @PostMapping("/clientes")
    public Map<String, Object> criar(@RequestBody ClienteService.ClienteForm form) {
        Cliente c = servico.criar(form, atual.id());
        return Map.of("id", c.getId(), "mensagem", "Cliente cadastrado. Anexe os documentos e confirme a entrevista.");
    }

    public record EntrevistaReq(String situacao) {
    }

    /** Lista enxuta para a tela Entrevistas. */
    @GetMapping("/entrevistas")
    public List<Map<String, Object>> entrevistas() {
        return servico.carregar(false).stream().map(a -> {
            ClienteDados c = a.cliente();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.id());
            m.put("nome", c.nome());
            m.put("municipio", c.municipio());
            m.put("colonia", c.colonia());
            m.put("etapa", a.etapaNome());
            m.put("etapaCor", a.etapaCor());
            m.put("entrevista", c.entrevista());
            return m;
        }).toList();
    }

    @PutMapping("/clientes/{id}/entrevista")
    public Map<String, String> entrevista(@PathVariable Long id, @RequestBody EntrevistaReq req) {
        Entrevista nova;
        try {
            nova = Entrevista.valueOf(req.situacao());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraException("Situação da entrevista inválida.");
        }
        servico.alterarEntrevista(id, nova, atual.id());
        return Map.of("mensagem", "Entrevista: " + nova.getNome() + ".");
    }

    @GetMapping("/clientes/{id}/senha-gov")
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> senhaGov(@PathVariable Long id) {
        return Map.of("senha", servico.revelarSenhaGov(id, atual.id()));
    }

    /** Ficha completa: análise + documentos + linha do tempo + DAE dos últimos 12 meses + honorários recebidos. */
    @GetMapping("/clientes/{id}")
    public Map<String, Object> ficha(@PathVariable Long id) {
        Analise a = servico.carregar(id);
        Map<Long, String> nomes = new HashMap<>();
        usuarios.findAll().forEach(u -> nomes.put(u.getId(), u.getNome()));
        ConfigValores cfg = config.valores();
        List<Map<String, Object>> dae = new ArrayList<>();
        List<String> comps = new ArrayList<>(Datas.competenciasDae(hoje.data()));
        java.util.Collections.reverse(comps);
        for (String comp : comps) {
            ClienteDados.DaeInfo d = a.cliente().daes().get(comp);
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("competencia", comp);
            l.put("label", Datas.competenciaLabel(comp));
            l.put("venda", d == null ? null : d.venda());
            l.put("valor", d == null ? null : d.valor());
            l.put("status", d == null ? null : d.status());
            l.put("vencimento", Datas.vencimentoDae(comp, cfg.diaVencimentoDae()));
            dae.add(l);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("analise", a);
        m.put("whatsapp", Telefone.whatsapp(a.cliente().telefone()));
        m.put("documentos", documentos.findByClienteIdOrderByEnviadoEmDesc(id).stream().map(d -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", d.getId());
            x.put("tipo", d.getTipo());
            x.put("tipoNome", d.getTipo().getNome());
            x.put("competencia", d.getCompetencia());
            x.put("nome", d.getNomeOriginal());
            x.put("enviadoEm", d.getEnviadoEm());
            x.put("usuario", nomes.get(d.getEnviadoPor()));
            return x;
        }).toList());
        m.put("eventos", eventos.findByClienteIdOrderByMomentoDescIdDesc(id).stream().map(e -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("momento", e.getMomento());
            x.put("tipo", e.getTipo());
            x.put("descricao", e.getDescricao());
            x.put("usuario", e.getUsuarioId() == null ? "sistema" : nomes.get(e.getUsuarioId()));
            return x;
        }).toList());
        m.put("dae", dae);
        if (atual.gestor()) {
            m.put("honorarios", honorarios.findByClienteIdOrderByDataRecebimentoDesc(id).stream().map(h -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("valor", h.getValor());
                x.put("data", h.getDataRecebimento());
                x.put("observacao", h.getObservacao());
                x.put("usuario", nomes.get(h.getUsuarioId()));
                return x;
            }).toList());
        }
        return m;
    }

    @PutMapping("/clientes/{id}")
    public Map<String, String> atualizar(@PathVariable Long id, @RequestBody ClienteService.ClienteForm form) {
        servico.atualizar(id, form, atual.id());
        return Map.of("mensagem", "Cadastro atualizado.");
    }

    /** Corpo: {"RGP1ANO": true, "HABITUAL": false, "GOV": null, ...} */
    @PutMapping("/clientes/{id}/requisitos")
    public Map<String, String> requisitos(@PathVariable Long id, @RequestBody Map<String, Boolean> corpo) {
        Map<Requisito, Boolean> r = new HashMap<>();
        corpo.forEach((k, v) -> r.put(Requisito.valueOf(k), v));
        servico.salvarRequisitos(id, r, atual.id());
        return Map.of("mensagem", "Requisitos salvos.");
    }

    @PostMapping("/clientes/{id}/arquivar")
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> arquivar(@PathVariable Long id) {
        servico.arquivar(id, atual.id());
        return Map.of("mensagem", "Situação do cliente alterada.");
    }

    @PostMapping("/clientes/{id}/status")
    public Map<String, String> status(@PathVariable Long id, @RequestBody ClienteService.StatusForm form) {
        return Map.of("mensagem", servico.atualizarStatus(id, form, atual.id()));
    }

    @PostMapping("/clientes/{id}/honorarios")
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> honorario(@PathVariable Long id, @RequestBody HonorarioReq req) {
        servico.registrarHonorario(id, req.valor(), req.data(), req.observacao(), atual.id());
        return Map.of("mensagem", "Recebimento registrado.");
    }

    // ------------------------------------------------------------------ documentos
    @PostMapping(value = "/clientes/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> anexar(@PathVariable Long id, @RequestParam String tipo,
                                      @RequestParam(required = false) String competencia,
                                      @RequestPart("arquivo") MultipartFile arquivo) {
        TipoDocumento t = TipoDocumento.valueOf(tipo);
        servico.anexar(id, t, competencia, arquivo, atual.id());
        return Map.of("mensagem", t.getNome() + " anexado.");
    }

    @GetMapping("/documentos/{id}")
    public ResponseEntity<Resource> baixar(@PathVariable Long id) {
        Documento d = documentos.findById(id).orElseThrow(() -> new NaoEncontradoException("Documento não encontrado"));
        Path p = arquivos.caminho(d.getClienteId(), d.getArquivo());
        if (!Files.exists(p)) {
            throw new NaoEncontradoException("Arquivo não encontrado no servidor.");
        }
        MediaType tipo = MediaTypeFactory.getMediaType(d.getNomeOriginal()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(tipo)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(d.getNomeOriginal(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .body(new FileSystemResource(p));
    }

    @DeleteMapping("/documentos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
    public Map<String, String> excluir(@PathVariable Long id) {
        servico.excluirDocumento(id, atual.id());
        return Map.of("mensagem", "Documento excluído.");
    }
}
