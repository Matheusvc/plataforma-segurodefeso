package br.com.segurodefeso.servico;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Criptografia (AES-GCM) de segredos guardados no banco, como a senha gov.br do cliente. */
@Service
public class SegredoService {

    private static final Logger log = LoggerFactory.getLogger(SegredoService.class);
    private static final String PADRAO_DEV = "chave-somente-para-desenvolvimento";
    private static final int IV = 12;

    private final SecretKeySpec chave;
    private final SecureRandom aleatorio = new SecureRandom();

    public SegredoService(@Value("${app.cripto-chave:}") String frase) {
        String f = frase == null || frase.isBlank() ? PADRAO_DEV : frase;
        if (f.equals(PADRAO_DEV)) {
            log.warn("APP_CRIPTO_CHAVE não definida: usando chave de desenvolvimento. Defina uma frase longa e secreta em produção.");
        }
        try {
            this.chave = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(f.getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[IV];
            aleatorio.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV + ct.length];
            System.arraycopy(iv, 0, out, 0, IV);
            System.arraycopy(ct, 0, out, IV, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new RegraException("Não foi possível proteger a senha.");
        }
    }

    public String decifrar(String cifrado) {
        try {
            byte[] in = Base64.getDecoder().decode(cifrado);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(128, in, 0, IV));
            return new String(c.doFinal(in, IV, in.length - IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RegraException("Não foi possível ler a senha: a chave de criptografia mudou?");
        }
    }
}
