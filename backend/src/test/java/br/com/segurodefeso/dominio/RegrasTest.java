package br.com.segurodefeso.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Testes das regras de negócio (não dependem do Spring nem do banco). */
class RegrasTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 9);
    static final ConfigValores CFG = ConfigValores.padrao();

    static ClienteDados cliente(Etapa etapa, String gov, Map<Requisito, Boolean> reqs, java.util.Set<TipoDocumento> docs,
                                Map<String, ClienteDados.DaeInfo> daes, int lib, int tot, BigDecimal recebido) {
        return cliente(etapa, gov, reqs, docs, daes, lib, tot, recebido, Entrevista.A_CONFIRMAR);
    }

    static ClienteDados cliente(Etapa etapa, String gov, Map<Requisito, Boolean> reqs, java.util.Set<TipoDocumento> docs,
                                Map<String, ClienteDados.DaeInfo> daes, int lib, int tot, BigDecimal recebido, Entrevista entrevista) {
        return new ClienteDados(1L, "Joana Pereira Matos", "52998224725", null, null, "(98) 98888-7777", null, "Raposa",
                "Colônia Z-53", "MA-1", "05/2019", LocalDate.of(2027, 12, 31), null, LocalDate.of(2026, 12, 2), gov, null,
                etapa, "2026/000999", null, "comprovante de residência", LocalDate.of(2026, 11, 19), "vínculo no CNIS", null,
                tot, lib, null, null, true, reqs, docs, daes, recebido, entrevista, ClienteDados.Pessoal.vazio());
    }

    static Map<Requisito, Boolean> todosSim() {
        Map<Requisito, Boolean> m = new EnumMap<>(Requisito.class);
        for (Requisito r : Requisito.values()) {
            m.put(r, true);
        }
        return m;
    }

    @Test
    void cpfDatasDinheiro() {
        assertTrue(Cpf.valido("529.982.247-25"));
        assertFalse(Cpf.valido("111.111.111-11"));
        assertFalse(Cpf.valido("529.982.247-26"));
        assertEquals("***.982.247-**", Cpf.mascarar("52998224725"));
        assertEquals(LocalDate.of(2026, 3, 5), Datas.ler("05/03/2026"));
        assertNull(Datas.ler("31/02/2026"));
        assertEquals(LocalDate.of(2027, 1, 20), Datas.vencimentoDae("2026-12", 20));
        assertEquals("nov/26", Datas.competenciaLabel("2026-11"));
        assertEquals(new BigDecimal("1234.56"), Dinheiro.ler("1.234,56"));
        assertEquals("R$ 1.234,50", Dinheiro.brl(new BigDecimal("1234.5")));
        assertEquals("5598988887777", Telefone.whatsapp("(98) 98888-7777"));
    }

    @Test
    void aptoQuandoTudoCompleto() {
        Analise a = Analisador.analisar(cliente(Etapa.A_REQUERER, "prata", todosSim(),
                EnumSet.copyOf(TipoDocumento.obrigatorios()), Map.of(), 0, 4, null), CFG, HOJE);
        assertTrue(a.apto(), a.bloqueios().toString());
        assertEquals(7, a.requisitosOk());
    }

    @Test
    void bloqueiosListamOQueFalta() {
        Map<Requisito, Boolean> reqs = todosSim();
        reqs.put(Requisito.SEM_VINCULO, false);
        reqs.remove(Requisito.GOV);
        Analise a = Analisador.analisar(cliente(Etapa.DOCUMENTACAO, "bronze", reqs, EnumSet.of(TipoDocumento.RG), Map.of(), 0, 4, null), CFG, HOJE);
        assertFalse(a.apto());
        assertTrue(a.essencialFalha());
        assertEquals(1, a.requisitosSemResposta());
        assertEquals(6, a.documentosPendentes().size());
        assertTrue(a.daeVencidos().isEmpty());          // sem entrevista confirmada não há DAE
        assertTrue(a.bloqueios().contains("conta gov.br sem nível prata/ouro"));
    }

    @Test
    void daeComecaEmMaioEsoParaEntrevistaConfirmada() {
        assertEquals(List.of("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11"),
                Datas.competenciasDae(HOJE));
        Map<String, ClienteDados.DaeInfo> daes = new HashMap<>();
        for (String comp : List.of("2026-05", "2026-06", "2026-07")) {
            daes.put(comp, new ClienteDados.DaeInfo(StatusDae.PAGO, null, null));
        }
        daes.put("2026-08", new ClienteDados.DaeInfo(StatusDae.DECLARADO, null, null));
        Analise a = Analisador.analisar(cliente(Etapa.DEFERIDO, "prata", todosSim(), EnumSet.noneOf(TipoDocumento.class),
                daes, 0, 4, null, Entrevista.CONFIRMADO), CFG, HOJE);
        assertEquals(1, a.daeVencidos().size());        // setembro venceu em 20/10
        assertEquals("2026-09", a.daeVencidos().get(0).competencia());
        assertEquals(2, a.daeAbertos().size());         // outubro e novembro ainda no prazo
        assertEquals(7, a.daeUltimosMeses().size());
        Analise nao = Analisador.analisar(cliente(Etapa.DEFERIDO, "prata", todosSim(), EnumSet.noneOf(TipoDocumento.class),
                Map.of(), 0, 4, null, Entrevista.NAO), CFG, HOJE);
        assertTrue(nao.daeVencidos().isEmpty());
        assertTrue(nao.daeAbertos().isEmpty());
        Prazos.calcular(List.of(a), List.of(), CFG, HOJE).forEach(p -> assertFalse(p.tipo().equals("DAE")));
    }

    @Test
    void honorarios() {
        Analise a = Analisador.analisar(cliente(Etapa.DEFERIDO, "prata", todosSim(), EnumSet.noneOf(TipoDocumento.class),
                Map.of(), 2, 4, new BigDecimal("486.30")), CFG, HOJE);
        assertEquals(new BigDecimal("486.30"), a.honorariosPorParcela());      // 30% de R$ 1.621
        assertEquals(0, a.honorariosDevidos().compareTo(new BigDecimal("972.60")));
        assertEquals(0, a.honorariosSaldo().compareTo(new BigDecimal("486.30")));
        assertEquals(0, a.honorariosFuturos().compareTo(new BigDecimal("972.60")));
    }

    @Test
    void prazosEFila() {
        Analise exig = Analisador.analisar(cliente(Etapa.EXIGENCIA, "prata", todosSim(),
                EnumSet.copyOf(TipoDocumento.obrigatorios()), Map.of(), 0, 4, null), CFG, HOJE);
        List<Prazos.Prazo> prazos = Prazos.calcular(List.of(exig), List.of(), CFG, HOJE);
        Prazos.Prazo p = prazos.stream().filter(x -> x.tipo().equals("Exigência")).findFirst().orElseThrow();
        assertEquals(10L, p.dias());
        assertEquals("em 10 dia(s)", p.falta());
        assertTrue(prazos.stream().anyMatch(x -> x.tipo().equals("Procuração")));   // vence em 23 dias
        List<FilaDoDia.Item> fila = FilaDoDia.calcular(List.of(exig), HOJE);
        assertEquals("Urgente", fila.get(0).prioridade());
        assertTrue(fila.stream().anyMatch(i -> i.titulo().startsWith("Conferir o status")));
    }

    @Test
    void mensagemETriagem() {
        Analise a = Analisador.analisar(cliente(Etapa.EXIGENCIA, "prata", todosSim(), EnumSet.of(TipoDocumento.RG),
                Map.of(), 0, 4, null), CFG, HOJE);
        String t = Mensagens.texto(Mensagens.Modelo.DOCS, a, CFG);
        assertTrue(t.startsWith("Olá, Joana!"));
        assertTrue(t.contains("Comprovante de residência"));
        String link = Mensagens.linkWhatsapp("(98) 98888-7777", t);
        assertNotNull(link);
        assertTrue(link.startsWith("https://wa.me/5598988887777?text="));
        Map<Requisito, Boolean> r = todosSim();
        assertEquals("Apto para requerer", Triagem.avaliar(r).titulo());
        r.put(Requisito.SEM_VINCULO, false);
        assertEquals("Não atende requisito essencial", Triagem.avaliar(r).titulo());
    }
}
