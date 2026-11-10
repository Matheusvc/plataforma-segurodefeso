package br.com.segurodefeso.servico;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Guarda os documentos anexados numa pasta fora da área pública (app.uploads/<cliente>/). */
@Service
public class ArquivoService {

    public static final Set<String> EXTENSOES = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".webp", ".heic");

    private final Path raiz;

    public ArquivoService(@Value("${app.uploads}") String pasta) throws IOException {
        this.raiz = Path.of(pasta).toAbsolutePath().normalize();
        Files.createDirectories(raiz);
    }

    public String salvar(Long clienteId, MultipartFile arquivo) {
        String original = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename();
        int ponto = original.lastIndexOf('.');
        String ext = ponto >= 0 ? original.substring(ponto).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSOES.contains(ext)) {
            throw new RegraException("Formato não aceito. Envie PDF ou foto (JPG, PNG, WEBP, HEIC).");
        }
        String nome = UUID.randomUUID().toString().replace("-", "") + ext;
        try (InputStream in = arquivo.getInputStream()) {
            Path pasta = raiz.resolve(String.valueOf(clienteId));
            Files.createDirectories(pasta);
            Files.copy(in, pasta.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RegraException("Não foi possível gravar o arquivo. Tente de novo.");
        }
        return nome;
    }

    // ------------------------------------------------------------------ foto de perfil (app.uploads/perfis/<usuário>/)
    public static final Set<String> EXTENSOES_FOTO = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final long MAX_FOTO = 3L * 1024 * 1024;

    public String salvarFoto(Long usuarioId, MultipartFile arquivo) {
        String original = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename();
        int ponto = original.lastIndexOf('.');
        String ext = ponto >= 0 ? original.substring(ponto).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSOES_FOTO.contains(ext)) {
            throw new RegraException("Envie a foto em JPG, PNG ou WEBP.");
        }
        if (arquivo.getSize() > MAX_FOTO) {
            throw new RegraException("A foto pode ter no máximo 3 MB.");
        }
        String nome = UUID.randomUUID().toString().replace("-", "") + ext;
        try (InputStream in = arquivo.getInputStream()) {
            Path pasta = pastaFoto(usuarioId);
            Files.createDirectories(pasta);
            Files.copy(in, pasta.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RegraException("Não foi possível gravar a foto. Tente de novo.");
        }
        return nome;
    }

    public Path caminhoFoto(Long usuarioId, String nome) {
        Path p = pastaFoto(usuarioId).resolve(nome).normalize();
        if (!p.startsWith(raiz)) {
            throw new RegraException("Caminho inválido.");
        }
        return p;
    }

    public void excluirFoto(Long usuarioId, String nome) {
        try {
            Files.deleteIfExists(caminhoFoto(usuarioId, nome));
        } catch (IOException ignored) {
            // foto órfã não atrapalha
        }
    }

    private Path pastaFoto(Long usuarioId) {
        return raiz.resolve("perfis").resolve(String.valueOf(usuarioId));
    }

    public Path caminho(Long clienteId, String nome) {
        Path p = raiz.resolve(String.valueOf(clienteId)).resolve(nome).normalize();
        if (!p.startsWith(raiz)) {
            throw new RegraException("Caminho inválido.");
        }
        return p;
    }

    public void excluir(Long clienteId, String nome) {
        try {
            Files.deleteIfExists(caminho(clienteId, nome));
        } catch (IOException ignored) {
            // o registro já foi removido; o arquivo órfão não atrapalha
        }
    }
}
