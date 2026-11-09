package br.com.segurodefeso.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.servico.NaoEncontradoException;
import br.com.segurodefeso.servico.PlanilhaService;
import br.com.segurodefeso.servico.UsuarioAtual;

@RestController
@RequestMapping("/api/planilhas")
public class PlanilhaController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PlanilhaService servico;
    private final UsuarioAtual atual;
    private final Hoje hoje;

    public PlanilhaController(PlanilhaService servico, UsuarioAtual atual, Hoje hoje) {
        this.servico = servico;
        this.atual = atual;
        this.hoje = hoje;
    }

    @GetMapping
    public List<PlanilhaService.Lista> listas() {
        boolean gestor = atual.gestor();
        return PlanilhaService.LISTAS.stream().filter(l -> gestor || !l.restrita()).toList();
    }

    @GetMapping("/exportar/{lista}.{formato}")
    public ResponseEntity<byte[]> exportar(@PathVariable String lista, @PathVariable String formato) {
        PlanilhaService.Lista item = PlanilhaService.LISTAS.stream().filter(l -> l.id().equals(lista)).findFirst()
            .orElseThrow(() -> new NaoEncontradoException("Lista não encontrada"));
        if (item.restrita() && !atual.gestor()) {
            throw new org.springframework.security.access.AccessDeniedException("restrito");
        }
        PlanilhaService.Tabela t = servico.tabela(lista);
        String nome = "seguro-defeso_" + lista + "_" + hoje.data() + "." + formato;
        byte[] corpo;
        MediaType tipo;
        if (formato.equals("xlsx")) {
            corpo = servico.xlsx(item.titulo(), t);
            tipo = XLSX;
        } else if (formato.equals("csv")) {
            corpo = servico.csv(t);
            tipo = new MediaType("text", "csv", StandardCharsets.UTF_8);
        } else {
            throw new NaoEncontradoException("Formato não suportado");
        }
        return arquivo(corpo, tipo, nome);
    }

    @GetMapping("/modelo.xlsx")
    public ResponseEntity<byte[]> modelo() {
        return arquivo(servico.modelo(), XLSX, "modelo_importacao_clientes.xlsx");
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlanilhaService.Previa previa(@RequestPart("arquivo") MultipartFile arquivo) {
        return servico.previa(arquivo, atual.id());
    }

    @PostMapping("/importar/{token}/confirmar")
    public Map<String, String> confirmar(@PathVariable String token) {
        return Map.of("mensagem", servico.confirmar(token, atual.id()));
    }

    private static ResponseEntity<byte[]> arquivo(byte[] corpo, MediaType tipo, String nome) {
        return ResponseEntity.ok()
            .contentType(tipo)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome, StandardCharsets.UTF_8).build().toString())
            .body(corpo);
    }
}
