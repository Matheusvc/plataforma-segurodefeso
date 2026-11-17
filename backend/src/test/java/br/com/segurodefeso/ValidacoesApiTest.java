package br.com.segurodefeso;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** Paginação das listas e validações de entrada (valores, datas, usuários, parâmetros, documentos e importação). */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:teste5;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.hoje=2026-11-09",
    "app.admin-email=admin@teste.com",
    "app.admin-senha=SenhaForte123",
    "app.uploads=target/uploads-teste5",
    "app.dados-exemplo=false"
})
@AutoConfigureMockMvc
@DirtiesContext
class ValidacoesApiTest {

    @Autowired
    MockMvc mvc;

    RequestPostProcessor admin() {
        return user("admin@teste.com").roles("ADMIN");
    }

    String criarCliente(String nome, String cpf) throws Exception {
        String resp = mvc.perform(post("/api/clientes").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"cpf\":\"" + cpf + "\",\"telefone\":\"(98) 98888-7777\","
                    + "\"municipio\":\"Raposa\",\"rgpNumero\":\"MA-1\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return resp.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    void json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String corpo, int esperado, String erro) throws Exception {
        var r = mvc.perform(req.with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().is(esperado));
        if (erro != null) {
            r.andExpect(jsonPath("$.erro", containsString(erro)));
        }
    }

    @Test
    void listasSaoPaginadasEFiltradasNoServidor() throws Exception {
        criarCliente("Ana Paginada Souza", "529.982.247-25");
        criarCliente("Bruno Paginado Lima", "111.444.777-35");
        criarCliente("Carla Paginada Dias", "390.533.447-05");

        mvc.perform(get("/api/clientes?tamanho=2").with(admin()))
            .andExpect(jsonPath("$.itens", hasSize(2)))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.itens[0].cliente.nome").value("Ana Paginada Souza"));
        mvc.perform(get("/api/clientes?tamanho=2&pagina=1").with(admin()))
            .andExpect(jsonPath("$.itens", hasSize(1)))
            .andExpect(jsonPath("$.itens[0].cliente.nome").value("Carla Paginada Dias"));
        mvc.perform(get("/api/clientes?q=bruno").with(admin()))
            .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/clientes?q=11144477735").with(admin()))
            .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/clientes").param("q", "_ru_o").with(admin()))   // _ e % não funcionam como curinga
            .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/clientes?tamanho=9999").with(admin()))      // tamanho limitado a 200
            .andExpect(jsonPath("$.tamanho").value(200));
        mvc.perform(get("/api/clientes?etapas=DOCUMENTACAO,A_REQUERER").with(admin()))
            .andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/api/clientes?etapas=INVENTADA").with(admin()))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/clientes?filtro=pendencia").with(admin()))
            .andExpect(jsonPath("$.total").value(3));

        mvc.perform(get("/api/entrevistas?tamanho=2").with(admin()))
            .andExpect(jsonPath("$.itens", hasSize(2)))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.totais.A_CONFIRMAR").value(3));
        mvc.perform(get("/api/prazos?limite=1").with(admin()))
            .andExpect(jsonPath("$.itens", hasSize(1)));
    }

    @Test
    void parametrosDoSistemaEUsuariosSaoValidados() throws Exception {
        String ok = "{\"nomeEscritorio\":\"Escritório Teste\",\"salarioMinimo\":\"1.621,00\",\"honorariosPct\":\"30\","
            + "\"aliquotaDae\":\"1,5\",\"diaVencimentoDae\":20,\"parcelasPadrao\":4}";
        json(put("/api/admin/configuracoes"), ok.replace("\"1,5\"", "\"150\""), 400, "Alíquota");
        json(put("/api/admin/configuracoes"), ok.replace("\"1.621,00\"", "\"abc\""), 400, "salário");
        json(put("/api/admin/configuracoes"), ok.replace("20,", "40,"), 400, "1 a 28");
        json(put("/api/admin/configuracoes"), ok, 200, null);

        String novo = "{\"nome\":\"Paulo Silva\",\"email\":\"paulo@escritorio.com.br\",\"senha\":\"SenhaBoa12345\",\"papel\":\"EQUIPE\"}";
        json(post("/api/admin/usuarios"), novo.replace("paulo@escritorio.com.br", "sem-arroba"), 400, "e-mail válido");
        json(post("/api/admin/usuarios"), novo.replace("paulo@escritorio.com.br", "a@b"), 400, "e-mail válido");
        json(post("/api/admin/usuarios"), novo.replace("EQUIPE", "CHEFE"), 400, "papel");
        json(post("/api/admin/usuarios"), novo.replace(",\"papel\":\"EQUIPE\"", ""), 400, "papel");
        json(post("/api/admin/usuarios"), novo.replace("SenhaBoa12345", "curta1"), 400, "Senha inicial fraca");
        json(post("/api/admin/usuarios"), novo.replace("Paulo Silva", "Pa"), 400, "nome completo");
        json(post("/api/admin/usuarios"), novo, 200, null);
        json(post("/api/admin/usuarios"), novo, 400, "Já existe");
    }

    @Test
    void honorariosStatusDaeEDefesosRecusamValoresInvalidos() throws Exception {
        String id = criarCliente("Dora Valores Teste", "168.995.350-09");

        // status: parcelas e datas
        String base = "\"sit\":\"deferido\"";
        json(post("/api/clientes/" + id + "/status"), "{" + base + ",\"parcelasTotal\":13}", 400, "1 a 12");
        json(post("/api/clientes/" + id + "/status"), "{" + base + ",\"parcelasTotal\":4,\"parcelasLiberadas\":5}", 400, "parcelas liberadas");
        json(post("/api/clientes/" + id + "/status"), "{\"sit\":\"indeferido\",\"motivo\":\"x\",\"ciencia\":\"31/02/2026\"}", 400, "ciência inválida");
        json(post("/api/clientes/" + id + "/status"), "{\"sit\":\"indeferido\",\"motivo\":\"x\",\"ciencia\":\"01/01/2027\"}", 400, "futura");
        json(post("/api/clientes/" + id + "/status"), "{" + base + ",\"parcelasTotal\":4,\"parcelasLiberadas\":2}", 200, null);

        // honorários: valor e data
        String hon = "/api/clientes/" + id + "/honorarios";
        json(post(hon), "{\"valor\":\"abc\"}", 400, "Valor inválido");
        json(post(hon), "{\"valor\":\"-10\"}", 400, "Valor inválido");
        json(post(hon), "{\"valor\":\"486,30\",\"data\":\"99/99/2026\"}", 400, "Data do recebimento inválida");
        json(post(hon), "{\"valor\":\"486,30\",\"data\":\"01/01/2027\"}", 400, "futura");
        json(post(hon), "{\"valor\":\"486,30\"}", 200, null);

        // DAE: venda inválida e competência do comprovante
        json(put("/api/clientes/" + id + "/entrevista"), "{\"situacao\":\"CONFIRMADO\"}", 200, null);
        json(post("/api/dae"), "{\"competencia\":\"2026-11\",\"itens\":[{\"clienteId\":" + id + ",\"venda\":\"1x00\",\"status\":\"PAGO\"}]}", 400, "Venda inválida de Dora");
        json(post("/api/dae"), "{\"competencia\":\"2026-11\",\"itens\":[{\"clienteId\":" + id + ",\"venda\":\"-5\",\"status\":\"PAGO\"}]}", 400, "Venda inválida");
        mvc.perform(multipart("/api/clientes/" + id + "/documentos").file(new MockMultipartFile("arquivo", "c.pdf", "application/pdf", new byte[] {1}))
                .param("tipo", "COMPROVANTE_DAE").with(admin()).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("competência do DAE")));

        // defesos: datas
        String def = "{\"especie\":\"Camarão\",\"inicio\":\"01/12/2026\",\"prazoRequerimento\":\"30/12/2026\"";
        json(post("/api/defesos"), def + ",\"fim\":\"32/01/2027\"}", 400, "Fim do defeso inválido");
        json(post("/api/defesos"), def + ",\"fim\":\"30/11/2026\"}", 400, "antes do início");
        json(post("/api/defesos"), def + ",\"fim\":\"31/01/2027\"}", 200, null);
    }

    @Test
    void importacaoAvisaCamposInvalidosELimitaAPrevia() throws Exception {
        String csv = "Nome;CPF;Telefone;NIT;CEP\nMaria da Silva Teste;453.178.287-91;123;4444;65\n";
        mvc.perform(multipart("/api/planilhas/importar").file(new MockMultipartFile("arquivo", "clientes.csv", "text/csv", csv.getBytes()))
                .with(admin()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLinhas").value(1))
            .andExpect(jsonPath("$.registros[0].avisos", hasItem("telefone inválido (ignorado)")))
            .andExpect(jsonPath("$.registros[0].avisos", hasItem("NIT/PIS inválido (ignorado)")))
            .andExpect(jsonPath("$.registros[0].avisos", hasItem("CEP inválido (ignorado)")));
    }
}
