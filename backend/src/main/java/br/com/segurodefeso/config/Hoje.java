package br.com.segurodefeso.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Data e hora "de hoje" no fuso do escritório. Pode ser fixada (app.hoje) para testes e demonstração. */
@Component
public class Hoje {

    private final String fixo;
    private final ZoneId zona;

    public Hoje(@Value("${app.hoje:}") String fixo, @Value("${app.zona:America/Fortaleza}") String zona) {
        this.fixo = fixo == null ? "" : fixo.trim();
        this.zona = ZoneId.of(zona);
    }

    public LocalDate data() {
        return fixo.isEmpty() ? LocalDate.now(zona) : LocalDate.parse(fixo);
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(zona).withNano(0);
    }
}
