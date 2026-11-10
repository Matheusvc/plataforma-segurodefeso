package br.com.segurodefeso;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Meu perfil: nome, telefone, foto e proteção contra arquivos que não são imagem. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:teste3;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.hoje=2026-11-09",
    "app.admin-email=admin@teste.com",
    "app.admin-senha=SenhaForte123",
    "app.uploads=target/uploads-teste3",
    "app.dados-exemplo=false"
})
@AutoConfigureMockMvc
@DirtiesContext
class PerfilApiTest {

    @Autowired
    MockMvc mvc;

    RequestPostProcessor admin() {
        return user("admin@teste.com").roles("ADMIN");
    }

    @Test
    void perfilEFoto() throws Exception {
        mvc.perform(put("/api/auth/perfil").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Matheus Castro\",\"telefone\":\"(98) 99999-0000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Matheus Castro"))
            .andExpect(jsonPath("$.telefone").value("(98) 99999-0000"));
        mvc.perform(put("/api/auth/perfil").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"  \"}"))
            .andExpect(status().isBadRequest());

        // sem foto ainda
        mvc.perform(get("/api/auth/foto").with(admin())).andExpect(status().isNotFound());

        // arquivo que não é imagem é recusado
        mvc.perform(multipart("/api/auth/foto").file(new MockMultipartFile("arquivo", "virus.exe", "application/octet-stream", new byte[] {1, 2}))
                .with(admin()).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("JPG, PNG ou WEBP")));

        // envia, lê e remove a foto
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        mvc.perform(multipart("/api/auth/foto").file(new MockMultipartFile("arquivo", "eu.png", "image/png", png))
                .with(admin()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.foto", containsString(".png")));
        mvc.perform(get("/api/auth/foto").with(admin()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG));
        mvc.perform(delete("/api/auth/foto").with(admin()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.foto").value(nullValue()));
        mvc.perform(get("/api/auth/eu").with(admin()))
            .andExpect(content().string(not(containsString(".png"))));
    }
}
