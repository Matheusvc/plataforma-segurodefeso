package br.com.segurodefeso;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/** Cadastro completo, entrevista, trava do DAE e senha gov protegida. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:teste2;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.hoje=2026-11-09",
    "app.admin-email=admin@teste.com",
    "app.admin-senha=SenhaForte123",
    "app.uploads=target/uploads-teste2",
    "app.dados-exemplo=false"
})
@AutoConfigureMockMvc
@DirtiesContext
class EntrevistaDaeApiTest {

    @Autowired
    MockMvc mvc;

    RequestPostProcessor admin() {
        return user("admin@teste.com").roles("ADMIN");
    }

    @Test
    void cadastroEntrevistaEDae() throws Exception {
        String resp = mvc.perform(post("/api/clientes").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Carlos Souza\",\"cpf\":\"111.444.777-35\",\"telefone\":\"(98) 97777-6666\","
                    + "\"municipio\":\"Raposa\",\"rgpNumero\":\"MA-2\",\"sexo\":\"Masculino\",\"nomeMae\":\"Ana Souza\","
                    + "\"cep\":\"65138-000\",\"uf\":\"ma\",\"dataAfiliacao\":\"15/03/2019\",\"senhaGov\":\"SegredoGov123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String id = resp.replaceAll(".*\"id\":(\\d+).*", "$1");

        // dados novos gravados; a senha nunca aparece nas respostas
        mvc.perform(get("/api/clientes/" + id).with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.analise.cliente.pessoal.nomeMae").value("Ana Souza"))
            .andExpect(jsonPath("$.analise.cliente.pessoal.cep").value("65138000"))
            .andExpect(jsonPath("$.analise.cliente.pessoal.uf").value("MA"))
            .andExpect(jsonPath("$.analise.cliente.pessoal.senhaGovCadastrada").value(true))
            .andExpect(jsonPath("$.analise.cliente.entrevista").value("A_CONFIRMAR"))
            .andExpect(content().string(not(containsString("SegredoGov123"))));
        mvc.perform(get("/api/clientes").with(admin()))
            .andExpect(content().string(not(containsString("SegredoGov123"))));

        // só gestor lê a senha
        mvc.perform(get("/api/clientes/" + id + "/senha-gov").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.senha").value("SegredoGov123"));
        mvc.perform(get("/api/clientes/" + id + "/senha-gov").with(user("eq@teste.com").roles("EQUIPE")))
            .andExpect(status().isForbidden());

        // sem entrevista confirmada não há DAE
        mvc.perform(get("/api/dae").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientes", hasSize(0)))
            .andExpect(jsonPath("$.competencias[0]").value("2026-05"));
        String dae = "{\"competencia\":\"2026-11\",\"itens\":[{\"clienteId\":" + id + ",\"venda\":\"1000,00\",\"status\":\"PAGO\"}]}";
        mvc.perform(post("/api/dae").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(dae))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("entrevista confirmada")));

        // confirma a entrevista e lança o DAE
        mvc.perform(put("/api/clientes/" + id + "/entrevista").with(admin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"CONFIRMADO\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/entrevistas").with(admin()))
            .andExpect(jsonPath("$[0].entrevista").value("CONFIRMADO"));
        mvc.perform(post("/api/dae").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(dae))
            .andExpect(status().isOk());
        mvc.perform(get("/api/dae?competencia=2026-11").with(admin()))
            .andExpect(jsonPath("$.clientes", hasSize(1)))
            .andExpect(jsonPath("$.resumo.PAGO").value(1))
            .andExpect(jsonPath("$.resumo.EM_ABERTO").value(0));

        // DAE não aparece em Prazos
        mvc.perform(get("/api/prazos?tipo=DAE").with(admin()))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void auxiliares() throws Exception {
        mvc.perform(post("/api/auxiliares").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Paula Lima\",\"telefone\":\"(98) 95555-4444\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/auxiliares").with(user("eq@teste.com").roles("EQUIPE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Paula Lima"));
        mvc.perform(post("/api/auxiliares").with(user("eq@teste.com").roles("EQUIPE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Outra\"}"))
            .andExpect(status().isForbidden());
    }
}
