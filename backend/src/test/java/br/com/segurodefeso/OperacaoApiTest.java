package br.com.segurodefeso;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.segurodefeso.dominio.Papel;
import br.com.segurodefeso.entidade.Usuario;
import br.com.segurodefeso.repositorio.UsuarioRepo;

/** Início, triagem, mensagens, honorários, relatórios, defesos e prazos pela API. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:teste4;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.hoje=2026-11-09",
    "app.admin-email=admin@teste.com",
    "app.admin-senha=SenhaForte123",
    "app.uploads=target/uploads-teste4",
    "app.dados-exemplo=false"
})
@AutoConfigureMockMvc
@DirtiesContext
class OperacaoApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepo usuarios;

    RequestPostProcessor admin() {
        return user("admin@teste.com").roles("ADMIN");
    }

    @BeforeEach
    void usuarioDeEquipe() {
        if (usuarios.findByEmailIgnoreCase("eq@teste.com").isEmpty()) {
            Usuario u = new Usuario();
            u.setNome("Equipe");
            u.setEmail("eq@teste.com");
            u.setSenhaHash("x");
            u.setPapel(Papel.EQUIPE);
            u.setAtivo(true);
            u.setCriadoEm(LocalDateTime.now());
            usuarios.save(u);
        }
    }

    @Test
    void triagemInicioMensagemHonorariosERelatorios() throws Exception {
        mvc.perform(post("/api/triagem/avaliar").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"respostas\":{}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").exists());

        String resp = mvc.perform(post("/api/clientes").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Rita Alves\",\"cpf\":\"529.982.247-25\",\"telefone\":\"(98) 97777-1111\","
                    + "\"municipio\":\"Raposa\",\"rgpNumero\":\"MA-5\",\"colonia\":\"Colônia Z-53\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String id = resp.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(get("/api/inicio").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kpis", hasSize(6)))
            .andExpect(jsonPath("$.funil", hasSize(6)))
            .andExpect(jsonPath("$.fila").isArray());
        mvc.perform(get("/api/inicio").with(user("eq@teste.com").roles("EQUIPE")))
            .andExpect(jsonPath("$.kpis", hasSize(5)));

        mvc.perform(get("/api/mensagens?cliente=" + id + "&modelo=DOCS").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.texto", startsWith("Olá, Rita!")))
            .andExpect(jsonPath("$.link", startsWith("https://wa.me/")));

        mvc.perform(get("/api/honorarios").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientes", hasSize(0)))
            .andExpect(jsonPath("$.salarioMinimo").value(1621.0));

        mvc.perform(get("/api/relatorios").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.colonias", hasSize(1)))
            .andExpect(jsonPath("$.colonias[0].colonia").value("Colônia Z-53"))
            .andExpect(jsonPath("$.indicadores", hasSize(4)))
            .andExpect(jsonPath("$.motivos").isArray());
        mvc.perform(get("/api/relatorios").with(user("eq@teste.com").roles("EQUIPE")))
            .andExpect(status().isForbidden());
    }

    @Test
    void defesosEPrazos() throws Exception {
        mvc.perform(post("/api/defesos").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"especie\":\"\",\"inicio\":\"01/12/2026\",\"prazoRequerimento\":\"30/12/2026\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/defesos").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"especie\":\"Caranguejo\",\"regiao\":\" \",\"portaria\":\"123\","
                    + "\"inicio\":\"01/12/2026\",\"fim\":\"31/01/2027\",\"prazoRequerimento\":\"30/12/2026\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/defesos").with(admin()))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].regiao").doesNotExist());
        mvc.perform(get("/api/prazos?tipo=Defeso").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itens[0].tipo").value("Defeso"));

        mvc.perform(delete("/api/defesos/1").with(user("eq@teste.com").roles("EQUIPE")).with(csrf()))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/defesos/1").with(admin()).with(csrf()))
            .andExpect(status().isOk());
        mvc.perform(get("/api/defesos").with(admin()))
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
