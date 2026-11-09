package br.com.segurodefeso;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Teste do fluxo principal pela API, com banco H2 em memória. Rode com: mvn test */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:teste;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.hoje=2026-11-09",
    "app.admin-email=admin@teste.com",
    "app.admin-senha=SenhaForte123",
    "app.uploads=target/uploads-teste"
})
@AutoConfigureMockMvc
@DirtiesContext
class FluxoApiTest {

    @Autowired
    MockMvc mvc;

    RequestPostProcessor admin() {
        return user("admin@teste.com").roles("ADMIN");
    }

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginComSenhaErradaEDepoisCerta() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@teste.com\",\"senha\":\"errada\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@teste.com\",\"senha\":\"SenhaForte123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.papel").value("ADMIN"));
    }

    @Test
    void cadastroStatusEPrazos() throws Exception {
        // CPF inválido é recusado
        mvc.perform(post("/api/clientes").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teste\",\"cpf\":\"123.456.789-00\",\"telefone\":\"98 99999-0000\",\"municipio\":\"Raposa\",\"rgpNumero\":\"MA-1\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("CPF inválido")));

        // cadastro válido
        String resp = mvc.perform(post("/api/clientes").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Joana Pereira Matos\",\"cpf\":\"529.982.247-25\",\"telefone\":\"(98) 98888-7777\","
                    + "\"municipio\":\"Raposa\",\"rgpNumero\":\"MA-000111\",\"govNivel\":\"prata\",\"procuracaoValidade\":\"31/12/2027\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String id = resp.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(get("/api/clientes").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].cliente.etapa").value("DOCUMENTACAO"))
            .andExpect(jsonPath("$[0].apto").value(false));

        // exigência com prazo aparece nos prazos
        mvc.perform(post("/api/clientes/" + id + "/status").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sit\":\"exigencia\",\"protocolo\":\"2026/000999\",\"exigenciaTexto\":\"comprovante de residência\",\"prazo\":\"19/11/2026\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/prazos?tipo=Exigência").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dias").value(10))
            .andExpect(jsonPath("$[0].falta").value("em 10 dia(s)"));

        // deferido -> honorários (30% de R$ 1.621 por parcela)
        mvc.perform(post("/api/clientes/" + id + "/status").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sit\":\"deferido\",\"parcelasTotal\":4,\"parcelasLiberadas\":2}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/honorarios").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.saldo").value(972.6));

        // equipe não acessa honorários
        mvc.perform(get("/api/honorarios").with(user("eq@teste.com").roles("EQUIPE")))
            .andExpect(status().isForbidden());

        // exportação em Excel
        mvc.perform(get("/api/planilhas/exportar/clientes.xlsx").with(admin()))
            .andExpect(status().isOk());
    }
}
