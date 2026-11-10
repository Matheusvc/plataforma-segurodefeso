package br.com.segurodefeso.servico;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.segurodefeso.config.Hoje;
import br.com.segurodefeso.dominio.Analise;
import br.com.segurodefeso.dominio.ClienteDados;
import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.dominio.Cpf;
import br.com.segurodefeso.dominio.Datas;
import br.com.segurodefeso.dominio.Etapa;
import br.com.segurodefeso.dominio.Prazos;
import br.com.segurodefeso.dominio.Requisito;
import br.com.segurodefeso.entidade.Cliente;
import br.com.segurodefeso.repositorio.ClienteRepo;
import br.com.segurodefeso.repositorio.DefesoRepo;

/** Exportar listas (.xlsx e .csv) e importar a planilha de clientes. */
@Service
public class PlanilhaService {

    public record Lista(String id, String titulo, String descricao, boolean restrita) {
    }

    public record Tabela(List<String> cabecalho, List<List<Object>> linhas, Set<Integer> colunasDinheiro) {
    }

    public record LinhaImportacao(int linha, Map<String, String> dados, List<String> erros, List<String> avisos, String acao) {
    }

    public record Previa(String token, String arquivo, List<String> colunas, List<String> naoEncontradas,
                         Map<String, Long> totais, List<LinhaImportacao> registros) {
    }

    private record PreviaGuardada(Long usuarioId, LocalDateTime criadaEm, Previa previa) {
    }

    public static final List<Lista> LISTAS = List.of(
        new Lista("clientes", "Cadastro de clientes", "Dados pessoais, RGP, procuração, conta gov.br, etapa e pendências.", false),
        new Lista("dae", "Controle de DAE", "Situação de cada competência desde maio, venda e valor do DAE.", false),
        new Lista("requerimentos", "Requerimentos", "Etapa, protocolo, exigência, prazo e situação de cada pedido.", false),
        new Lista("prazos", "Prazos", "Vencimentos: exigência, recurso, RGP, procuração e defeso.", false),
        new Lista("honorarios", "Honorários", "Parcelas liberadas, honorários devidos, recebidos e saldo.", true));

    /** Cabeçalhos aceitos na importação (sem acento, minúsculos, só letras e números). */
    private static final Map<String, List<String>> COLUNAS = new LinkedHashMap<>();
    private static final Map<String, String> ROTULOS = new LinkedHashMap<>();

    static {
        col("nome", "Nome", "nome", "nomecompleto", "cliente", "pescador");
        col("cpf", "CPF", "cpf");
        col("nit", "NIT", "nit", "pis", "nitpis", "pisnit");
        col("nascimento", "Nascimento", "nascimento", "datanascimento", "datadenascimento", "dn");
        col("telefone", "Telefone", "telefone", "celular", "whatsapp", "fone", "contato");
        col("endereco", "Endereço", "endereco");
        col("municipio", "Município", "municipio", "cidade");
        col("colonia", "Colônia", "colonia", "colonias");
        col("rgpNumero", "Nº RGP", "nrgp", "numerorgp", "rgp", "registrorgp");
        col("rgpDesde", "RGP desde", "rgpdesde", "dataregistro", "registrodesde", "datargp", "datadorgp");
        col("rgpValidade", "Validade RGP", "validadergp", "rgpvalidade", "vencimentorgp");
        col("especie", "Espécie", "especie", "modalidade");
        col("procuracaoValidade", "Validade procuração", "validadeprocuracao", "procuracao", "procuracaoecac", "validadeprocuracaoecac");
        col("govNivel", "Conta gov.br", "contagovbr", "govbr", "nivelgovbr", "gov");
        col("etapa", "Etapa", "etapa", "situacao", "status");
        col("protocolo", "Protocolo", "protocolo");
        // Campos do cadastro completo. Ficam depois dos antigos para que "rg" não "roube" colunas de RGP.
        col("sexo", "Sexo", "sexo", "genero");
        col("rg", "RG", "rg", "identidade");
        col("orgaoEmissor", "Órgão emissor", "orgaoemissor", "orgaoexpedidor", "emissor");
        col("dataExpedicao", "Data da expedição", "dataexpedicao", "dataemissao", "expedicao");
        col("nomePai", "Nome do pai", "nomepai", "pai");
        col("nomeMae", "Nome da mãe", "nomemae", "mae");
        col("estadoCivil", "Estado civil", "estadocivil", "statuscivil");
        col("estadoOrigem", "Estado de origem", "estadoorigem", "ufnascimento", "naturalidadeuf");
        col("cidadeOrigem", "Cidade de origem", "cidadeorigem", "naturalidade");
        col("tituloEleitor", "Título de eleitor", "tituloeleitor", "tituloeleitoral", "titulo");
        col("inscricaoNumero", "Nº da inscrição", "numeroinscricao", "numeracaoinscricao", "inscricao");
        col("dataAfiliacao", "Data da afiliação", "dataafiliacao", "afiliacao");
        col("cep", "CEP", "cep");
        col("logradouro", "Logradouro", "logradouro", "rua");
        col("numero", "Número", "numero", "num");
        col("complemento", "Complemento", "complemento");
        col("bairro", "Bairro", "bairro");
        col("uf", "UF", "uf", "siglauf");
    }

    private static void col(String campo, String rotulo, String... nomes) {
        COLUNAS.put(campo, List.of(nomes));
        ROTULOS.put(campo, rotulo);
    }

    private final ClienteService clienteService;
    private final ClienteRepo clientes;
    private final DefesoRepo defesos;
    private final ConfigService config;
    private final EventoService eventos;
    private final Hoje hoje;
    private final Map<String, PreviaGuardada> previas = new ConcurrentHashMap<>();

    public PlanilhaService(ClienteService clienteService, ClienteRepo clientes, DefesoRepo defesos, ConfigService config,
                           EventoService eventos, Hoje hoje) {
        this.clienteService = clienteService;
        this.clientes = clientes;
        this.defesos = defesos;
        this.config = config;
        this.eventos = eventos;
        this.hoje = hoje;
    }

    // ================================================================== exportação
    @Transactional(readOnly = true)
    public Tabela tabela(String lista) {
        List<Analise> todos = clienteService.carregar(false);
        ConfigValores cfg = config.valores();
        return switch (lista) {
            case "clientes" -> new Tabela(List.of("Nome", "CPF", "NIT", "Nascimento", "Telefone", "Endereço", "Município", "Colônia",
                "Nº RGP", "RGP desde", "Validade RGP", "Espécie", "Validade procuração e-CAC", "Conta gov.br", "Etapa", "Protocolo",
                "Requisitos atendidos", "Pendências para requerer", "Entrevista", "Sexo", "RG", "Órgão emissor",
                "Data da expedição", "Nome do pai", "Nome da mãe", "Estado civil", "Estado de origem", "Cidade de origem",
                "Título de eleitor", "Nº da inscrição", "Data da afiliação", "CEP", "Logradouro", "Número", "Complemento",
                "Bairro", "UF"),
                todos.stream().map(a -> {
                    ClienteDados c = a.cliente();
                    ClienteDados.Pessoal p = c.pessoal();
                    return lista(c.nome(), Cpf.formatar(c.cpf()), c.nit(), Datas.br(c.nascimento()), c.telefone(), c.endereco(),
                        c.municipio(), c.colonia(), c.rgpNumero(), c.rgpDesde(), Datas.br(c.rgpValidade()), c.especie(),
                        Datas.br(c.procuracaoValidade()), c.govNivel(), a.etapaNome(), c.protocolo(),
                        a.requisitosOk() + "/" + Requisito.values().length, String.join("; ", a.bloqueios()),
                        c.entrevista().getNome(), p.sexo(), p.rg(), p.orgaoEmissor(), Datas.br(p.dataExpedicao()), p.nomePai(),
                        p.nomeMae(), p.estadoCivil(), p.estadoOrigem(), p.cidadeOrigem(), p.tituloEleitor(), p.inscricaoNumero(),
                        Datas.br(p.dataAfiliacao()), p.cep(), p.logradouro(), p.numero(), p.complemento(), p.bairro(), p.uf());
                }).toList(), Set.of());
            case "dae" -> {
                List<String> comps = Datas.competenciasDae(hoje.data());
                String ult = comps.get(comps.size() - 1);
                List<String> cab = new ArrayList<>(List.of("Nome", "CPF", "Procuração e-CAC"));
                comps.forEach(x -> cab.add(Datas.competenciaLabel(x)));
                cab.add("Venda " + Datas.competenciaLabel(ult) + " (R$)");
                cab.add("DAE " + Datas.competenciaLabel(ult) + " (R$)");
                List<List<Object>> linhas = todos.stream().map(a -> {
                    ClienteDados c = a.cliente();
                    List<Object> l = lista(c.nome(), Cpf.formatar(c.cpf()),
                        c.procuracaoValidade() != null ? Datas.br(c.procuracaoValidade()) : "sem procuração");
                    for (String x : comps) {
                        ClienteDados.DaeInfo d = c.daes().get(x);
                        l.add(d == null ? "" : d.status().getNome());
                    }
                    ClienteDados.DaeInfo d = c.daes().get(ult);
                    l.add(d == null ? null : d.venda());
                    l.add(d == null ? null : d.valor());
                    return l;
                }).toList();
                yield new Tabela(cab, linhas, Set.of(cab.size() - 2, cab.size() - 1));
            }
            case "requerimentos" -> new Tabela(List.of("Nome", "CPF", "Etapa", "Protocolo", "Exigência", "Prazo da exigência",
                "Motivo do indeferimento", "Ciência do indeferimento", "Parcelas liberadas", "Total de parcelas",
                "Última conferência", "Situação"),
                todos.stream().map(a -> {
                    ClienteDados c = a.cliente();
                    return lista(c.nome(), Cpf.formatar(c.cpf()), a.etapaNome(), c.protocolo(), c.exigenciaTexto(),
                        Datas.br(c.prazoData()), c.indeferimentoMotivo(), Datas.br(c.cienciaData()), c.parcelasLiberadas(),
                        c.parcelasTotal(), Datas.br(c.ultimaConferencia()), c.situacao());
                }).toList(), Set.of());
            case "prazos" -> {
                List<Prazos.DefesoInfo> ds = defesos.findAll().stream().map(d -> new Prazos.DefesoInfo(d.getEspecie(),
                    d.getRegiao(), d.getPortaria(), d.getInicio(), d.getFim(), d.getPrazoRequerimento())).toList();
                yield new Tabela(List.of("Data", "Tipo", "Cliente", "Descrição", "Situação"),
                    Prazos.calcular(todos, ds, hoje.data()).stream()
                        .map(p -> lista(p.dataBr(), p.tipo(), p.cliente(), p.texto(), p.falta())).toList(), Set.of());
            }
            case "honorarios" -> new Tabela(List.of("Nome", "CPF", "Parcelas liberadas", "Total de parcelas", "% honorários",
                "Honorário por parcela (R$)", "Devido (R$)", "Recebido (R$)", "Saldo (R$)", "A liberar (R$)"),
                todos.stream().filter(a -> a.cliente().etapa() == Etapa.DEFERIDO && a.cliente().parcelasTotal() > 0)
                    .map(a -> lista(a.nome(), Cpf.formatar(a.cliente().cpf()), a.cliente().parcelasLiberadas(),
                        a.cliente().parcelasTotal(), a.honorariosPct(), a.honorariosPorParcela(), a.honorariosDevidos(),
                        a.honorariosRecebidos(), a.honorariosSaldo(), a.honorariosFuturos())).toList(),
                Set.of(5, 6, 7, 8, 9));
            default -> throw new NaoEncontradoException("Lista não encontrada");
        };
    }

    private static List<Object> lista(Object... v) {
        List<Object> l = new ArrayList<>();
        for (Object o : v) {
            l.add(o);
        }
        return l;
    }

    public byte[] xlsx(String titulo, Tabela t) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(titulo.length() > 31 ? titulo.substring(0, 31) : titulo);
            CellStyle cab = wb.createCellStyle();
            Font fonte = wb.createFont();
            fonte.setBold(true);
            fonte.setColor(IndexedColors.WHITE.getIndex());
            cab.setFont(fonte);
            cab.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            cab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle dinheiro = wb.createCellStyle();
            dinheiro.setDataFormat(wb.createDataFormat().getFormat("\"R$\" #,##0.00"));
            Row r0 = sh.createRow(0);
            for (int i = 0; i < t.cabecalho().size(); i++) {
                Cell c = r0.createCell(i);
                c.setCellValue(t.cabecalho().get(i));
                c.setCellStyle(cab);
            }
            int ln = 1;
            for (List<Object> linha : t.linhas()) {
                Row r = sh.createRow(ln++);
                for (int i = 0; i < linha.size(); i++) {
                    Object v = linha.get(i);
                    if (v == null) {
                        continue;
                    }
                    Cell c = r.createCell(i);
                    if (v instanceof BigDecimal b) {
                        c.setCellValue(b.doubleValue());
                        if (t.colunasDinheiro().contains(i)) {
                            c.setCellStyle(dinheiro);
                        }
                    } else if (v instanceof Number n) {
                        c.setCellValue(n.doubleValue());
                    } else {
                        c.setCellValue(String.valueOf(v));
                    }
                }
            }
            for (int i = 0; i < t.cabecalho().size(); i++) {
                int larg = t.cabecalho().get(i).length();
                for (List<Object> linha : t.linhas()) {
                    Object v = i < linha.size() ? linha.get(i) : null;
                    larg = Math.max(larg, v == null ? 0 : String.valueOf(v).length());
                }
                sh.setColumnWidth(i, Math.min(Math.max(10, larg + 2), 60) * 256);
            }
            sh.createFreezePane(0, 1);
            if (!t.cabecalho().isEmpty()) {
                sh.setAutoFilter(new CellRangeAddress(0, Math.max(0, t.linhas().size()), 0, t.cabecalho().size() - 1));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RegraException("Não foi possível gerar a planilha.");
        }
    }

    public byte[] csv(Tabela t) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(linhaCsv(new ArrayList<>(t.cabecalho())));
        for (List<Object> l : t.linhas()) {
            sb.append(linhaCsv(l));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String linhaCsv(List<?> valores) {
        List<String> partes = new ArrayList<>();
        for (Object v : valores) {
            String s = v == null ? "" : (v instanceof BigDecimal b ? b.toPlainString().replace('.', ',') : String.valueOf(v));
            if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
                s = "\"" + s.replace("\"", "\"\"") + "\"";
            }
            partes.add(s);
        }
        return String.join(";", partes) + "\r\n";
    }

    public byte[] modelo() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Clientes");
            String[] cab = {"Nome", "CPF", "NIT", "Nascimento", "Telefone", "Endereço", "Município", "Colônia", "Nº RGP",
                "RGP desde", "Validade RGP", "Espécie", "Validade procuração", "Conta gov.br", "Etapa", "Protocolo",
                "Sexo", "RG", "Órgão emissor", "Data da expedição", "Nome do pai", "Nome da mãe", "Estado civil",
                "Estado de origem", "Cidade de origem", "Título de eleitor", "Nº da inscrição", "Data da afiliação", "CEP",
                "Logradouro", "Número", "Complemento", "Bairro", "UF"};
            String[] ex = {"Exemplo da Silva", "529.982.247-25", "000.00000.00-0", "01/01/1980", "(98) 90000-0000", "",
                "Raposa", "Colônia Z-53", "MA-000000", "01/2020", "31/12/2027", "Caranguejo", "31/12/2027", "Prata", "Documentação", "",
                "Masculino", "1234567", "SSP", "10/05/2005", "José da Silva", "Maria da Silva", "Solteiro", "MA", "Raposa",
                "123456789012", "0001", "15/03/2019", "65138000", "Rua da Praia", "1", "", "Centro", "MA"};
            CellStyle texto = wb.createCellStyle();
            texto.setDataFormat(wb.createDataFormat().getFormat("@"));
            Row r0 = sh.createRow(0);
            Row r1 = sh.createRow(1);
            for (int i = 0; i < cab.length; i++) {
                r0.createCell(i).setCellValue(cab[i]);
                r1.createCell(i).setCellValue(ex[i]);
                sh.setColumnWidth(i, 20 * 256);
                sh.setDefaultColumnStyle(i, texto);
            }
            Sheet ajuda = wb.createSheet("Como preencher");
            String[] dicas = {
                "Obrigatórias: Nome e CPF. As demais são aproveitadas se existirem; a ordem das colunas não importa.",
                "Datas no formato dd/mm/aaaa. 'RGP desde' pode ser mês/ano (ex.: 05/2019).",
                "Conta gov.br: Bronze, Prata ou Ouro.",
                "Etapa: Documentação, A requerer, Requerido, Exigência, Deferido ou Indeferido.",
                "CPF já cadastrado: a importação só preenche os campos que estiverem vazios na plataforma.",
                "Apague a linha de exemplo antes de importar."};
            for (int i = 0; i < dicas.length; i++) {
                ajuda.createRow(i).createCell(0).setCellValue(dicas[i]);
            }
            ajuda.setColumnWidth(0, 110 * 256);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RegraException("Não foi possível gerar a planilha modelo.");
        }
    }

    // ================================================================== importação
    public Previa previa(MultipartFile arquivo, Long usuarioId) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraException("Escolha a planilha.");
        }
        String nome = arquivo.getOriginalFilename() == null ? "planilha" : arquivo.getOriginalFilename();
        List<List<String>> linhas = ler(arquivo, nome.toLowerCase(Locale.ROOT));
        if (linhas.size() < 2) {
            throw new RegraException("A planilha está vazia ou sem linha de cabeçalho.");
        }
        Map<String, Integer> mapa = mapear(linhas.get(0));
        if (!mapa.containsKey("nome") || !mapa.containsKey("cpf")) {
            throw new RegraException("A planilha precisa ter as colunas Nome e CPF. Use a planilha modelo como referência.");
        }
        Set<String> existentes = new HashSet<>();
        clientes.findAll().forEach(c -> existentes.add(c.getCpf()));
        Set<String> vistos = new HashSet<>();
        List<LinhaImportacao> registros = new ArrayList<>();
        for (int n = 1; n < linhas.size(); n++) {
            List<String> linha = linhas.get(n);
            Map<String, String> d = new LinkedHashMap<>();
            mapa.forEach((campo, i) -> d.put(campo, i < linha.size() ? linha.get(i).trim() : ""));
            String cpf = Cpf.digitos(d.get("cpf"));
            if (cpf.length() == 10) {
                cpf = "0" + cpf;          // o Excel costuma cortar o zero à esquerda
            }
            d.put("cpf", cpf);
            List<String> erros = new ArrayList<>();
            List<String> avisos = new ArrayList<>();
            if (d.getOrDefault("nome", "").isBlank()) erros.add("sem nome");
            if (!Cpf.valido(cpf)) erros.add("CPF inválido");
            else if (!vistos.add(cpf)) erros.add("CPF repetido na planilha");
            for (String campo : List.of("nascimento", "rgpValidade", "procuracaoValidade", "dataExpedicao", "dataAfiliacao")) {
                String v = d.get(campo);
                if (v != null && !v.isBlank() && Datas.ler(v) == null) {
                    avisos.add(ROTULOS.get(campo) + " com data inválida (ignorada)");
                    d.put(campo, "");
                }
            }
            if (d.getOrDefault("municipio", "").isBlank()) avisos.add("sem município");
            if (d.getOrDefault("rgpNumero", "").isBlank()) avisos.add("sem nº do RGP");
            String acao = !erros.isEmpty() ? "erro" : (existentes.contains(cpf) ? "atualizar" : "novo");
            registros.add(new LinhaImportacao(n + 1, d, erros, avisos, acao));
        }
        Map<String, Long> totais = new LinkedHashMap<>();
        for (String k : List.of("novo", "atualizar", "erro")) {
            totais.put(k, registros.stream().filter(r -> r.acao().equals(k)).count());
        }
        String token = UUID.randomUUID().toString();
        Previa p = new Previa(token, nome, mapa.keySet().stream().map(ROTULOS::get).toList(),
            COLUNAS.keySet().stream().filter(c -> !mapa.containsKey(c)).map(ROTULOS::get).toList(), totais, registros);
        previas.entrySet().removeIf(e -> e.getValue().criadaEm().isBefore(hoje.agora().minusMinutes(30)));
        previas.put(token, new PreviaGuardada(usuarioId, hoje.agora(), p));
        return p;
    }

    @Transactional
    public String confirmar(String token, Long usuarioId) {
        PreviaGuardada g = previas.remove(token);
        if (g == null) {
            throw new RegraException("A prévia expirou. Envie a planilha de novo.");
        }
        if (!g.usuarioId().equals(usuarioId)) {
            throw new RegraException("Esta prévia foi gerada por outro usuário.");
        }
        int novos = 0;
        int atualizados = 0;
        int parcelasPadrao = config.valores().parcelasPadrao();
        for (LinhaImportacao r : g.previa().registros()) {
            if (r.acao().equals("erro")) {
                continue;
            }
            Map<String, String> d = r.dados();
            Cliente existente = clientes.findByCpf(d.get("cpf")).orElse(null);
            if (existente != null) {
                List<String> preenchidos = completar(existente, d);
                if (!preenchidos.isEmpty()) {
                    existente.setAtualizadoEm(hoje.agora());
                    clientes.save(existente);
                    eventos.registrar(existente.getId(), usuarioId, "importacao", "Dados completados pela planilha "
                        + g.previa().arquivo() + ": " + String.join(", ", preenchidos));
                    atualizados++;
                }
            } else {
                Cliente c = new Cliente();
                c.setNome(d.get("nome"));
                c.setCpf(d.get("cpf"));
                completar(c, d);
                Etapa etapa = etapa(d.get("etapa"));
                c.setEtapa(etapa == null ? Etapa.DOCUMENTACAO : etapa);
                c.setParcelasTotal(parcelasPadrao);
                c.setAtivo(true);
                c.setCriadoEm(hoje.agora());
                c.setAtualizadoEm(hoje.agora());
                c = clientes.save(c);
                eventos.registrar(c.getId(), usuarioId, "importacao", "Cliente importado da planilha " + g.previa().arquivo());
                novos++;
            }
        }
        return "Importação concluída: " + novos + " cliente(s) novo(s), " + atualizados + " atualizado(s). "
            + "Os novos entram em Documentação: responda os requisitos e anexe os documentos de cada um.";
    }

    /** Preenche só os campos vazios. Devolve os rótulos dos campos preenchidos. */
    private static List<String> completar(Cliente c, Map<String, String> d) {
        List<String> feitos = new ArrayList<>();
        if (vazio(c.getNit()) && !vazio(d.get("nit"))) { c.setNit(d.get("nit")); feitos.add("NIT"); }
        if (c.getNascimento() == null && Datas.ler(d.get("nascimento")) != null) { c.setNascimento(Datas.ler(d.get("nascimento"))); feitos.add("Nascimento"); }
        if (vazio(c.getTelefone()) && !vazio(d.get("telefone"))) { c.setTelefone(d.get("telefone")); feitos.add("Telefone"); }
        if (vazio(c.getEndereco()) && !vazio(d.get("endereco"))) { c.setEndereco(d.get("endereco")); feitos.add("Endereço"); }
        if (vazio(c.getMunicipio()) && !vazio(d.get("municipio"))) { c.setMunicipio(d.get("municipio")); feitos.add("Município"); }
        if (vazio(c.getColonia()) && !vazio(d.get("colonia"))) { c.setColonia(d.get("colonia")); feitos.add("Colônia"); }
        if (vazio(c.getRgpNumero()) && !vazio(d.get("rgpNumero"))) { c.setRgpNumero(d.get("rgpNumero")); feitos.add("Nº RGP"); }
        if (vazio(c.getRgpDesde()) && !vazio(d.get("rgpDesde"))) { c.setRgpDesde(d.get("rgpDesde")); feitos.add("RGP desde"); }
        if (c.getRgpValidade() == null && Datas.ler(d.get("rgpValidade")) != null) { c.setRgpValidade(Datas.ler(d.get("rgpValidade"))); feitos.add("Validade RGP"); }
        if (vazio(c.getEspecie()) && !vazio(d.get("especie"))) { c.setEspecie(d.get("especie")); feitos.add("Espécie"); }
        if (c.getProcuracaoValidade() == null && Datas.ler(d.get("procuracaoValidade")) != null) {
            c.setProcuracaoValidade(Datas.ler(d.get("procuracaoValidade")));
            feitos.add("Validade procuração");
        }
        String gov = normalizar(d.get("govNivel"));
        if (vazio(c.getGovNivel()) && Set.of("bronze", "prata", "ouro").contains(gov)) { c.setGovNivel(gov); feitos.add("Conta gov.br"); }
        if (vazio(c.getProtocolo()) && !vazio(d.get("protocolo"))) { c.setProtocolo(d.get("protocolo")); feitos.add("Protocolo"); }
        if (vazio(c.getSexo()) && !vazio(d.get("sexo"))) { c.setSexo(d.get("sexo")); feitos.add("Sexo"); }
        if (vazio(c.getRg()) && !vazio(d.get("rg"))) { c.setRg(d.get("rg")); feitos.add("RG"); }
        if (vazio(c.getOrgaoEmissor()) && !vazio(d.get("orgaoEmissor"))) { c.setOrgaoEmissor(d.get("orgaoEmissor")); feitos.add("Órgão emissor"); }
        if (c.getDataExpedicao() == null && Datas.ler(d.get("dataExpedicao")) != null) { c.setDataExpedicao(Datas.ler(d.get("dataExpedicao"))); feitos.add("Data da expedição"); }
        if (vazio(c.getNomePai()) && !vazio(d.get("nomePai"))) { c.setNomePai(d.get("nomePai")); feitos.add("Nome do pai"); }
        if (vazio(c.getNomeMae()) && !vazio(d.get("nomeMae"))) { c.setNomeMae(d.get("nomeMae")); feitos.add("Nome da mãe"); }
        if (vazio(c.getEstadoCivil()) && !vazio(d.get("estadoCivil"))) { c.setEstadoCivil(d.get("estadoCivil")); feitos.add("Estado civil"); }
        String ufOrigem = siglaUf(d.get("estadoOrigem"));
        if (vazio(c.getEstadoOrigem()) && ufOrigem != null) { c.setEstadoOrigem(ufOrigem); feitos.add("Estado de origem"); }
        if (vazio(c.getCidadeOrigem()) && !vazio(d.get("cidadeOrigem"))) { c.setCidadeOrigem(d.get("cidadeOrigem")); feitos.add("Cidade de origem"); }
        if (vazio(c.getTituloEleitor()) && !vazio(d.get("tituloEleitor"))) { c.setTituloEleitor(d.get("tituloEleitor")); feitos.add("Título de eleitor"); }
        if (vazio(c.getInscricaoNumero()) && !vazio(d.get("inscricaoNumero"))) { c.setInscricaoNumero(d.get("inscricaoNumero")); feitos.add("Nº da inscrição"); }
        if (c.getDataAfiliacao() == null && Datas.ler(d.get("dataAfiliacao")) != null) { c.setDataAfiliacao(Datas.ler(d.get("dataAfiliacao"))); feitos.add("Data da afiliação"); }
        String cep = d.getOrDefault("cep", "").replaceAll("\\D", "");
        if (vazio(c.getCep()) && cep.length() == 8) { c.setCep(cep); feitos.add("CEP"); }
        if (vazio(c.getLogradouro()) && !vazio(d.get("logradouro"))) { c.setLogradouro(d.get("logradouro")); feitos.add("Logradouro"); }
        if (vazio(c.getNumero()) && !vazio(d.get("numero"))) { c.setNumero(d.get("numero")); feitos.add("Número"); }
        if (vazio(c.getComplemento()) && !vazio(d.get("complemento"))) { c.setComplemento(d.get("complemento")); feitos.add("Complemento"); }
        if (vazio(c.getBairro()) && !vazio(d.get("bairro"))) { c.setBairro(d.get("bairro")); feitos.add("Bairro"); }
        String uf = siglaUf(d.get("uf"));
        if (vazio(c.getUf()) && uf != null) { c.setUf(uf); feitos.add("UF"); }
        return feitos;
    }

    private static String siglaUf(String txt) {
        if (vazio(txt)) return null;
        String u = txt.trim().toUpperCase(Locale.ROOT);
        return u.matches("[A-Z]{2}") ? u : null;
    }

    static Etapa etapa(String txt) {
        String t = normalizar(txt);
        if (t.isEmpty()) return null;
        if (t.contains("indefer") || t.contains("negad")) return Etapa.INDEFERIDO;
        if (t.contains("defer") || t.contains("aprovad")) return Etapa.DEFERIDO;
        if (t.contains("exig")) return Etapa.EXIGENCIA;
        if (Set.of("arequerer", "requerer", "apto", "pronto").contains(t)) return Etapa.A_REQUERER;
        if (t.contains("requerid") || t.contains("protocol") || t.contains("analise")) return Etapa.REQUERIDO;
        return Etapa.DOCUMENTACAO;
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    static String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9]", "");
    }

    static Map<String, Integer> mapear(List<String> cabecalho) {
        List<String> norm = cabecalho.stream().map(PlanilhaService::normalizar).toList();
        Map<String, Integer> mapa = new LinkedHashMap<>();
        Set<Integer> usados = new HashSet<>();
        for (Map.Entry<String, List<String>> e : COLUNAS.entrySet()) {
            Integer idx = null;
            for (String nome : e.getValue()) {
                int i = norm.indexOf(nome);
                if (i >= 0 && !usados.contains(i)) { idx = i; break; }
            }
            if (idx == null) {
                outer:
                for (int i = 0; i < norm.size(); i++) {
                    if (usados.contains(i)) continue;
                    for (String nome : e.getValue()) {
                        if (norm.get(i).startsWith(nome)) { idx = i; break outer; }
                    }
                }
            }
            if (idx != null) {
                mapa.put(e.getKey(), idx);
                usados.add(idx);
            }
        }
        return mapa;
    }

    private List<List<String>> ler(MultipartFile arquivo, String nome) {
        try (InputStream in = arquivo.getInputStream()) {
            if (nome.endsWith(".xlsx")) {
                return lerXlsx(in);
            }
            if (nome.endsWith(".csv") || nome.endsWith(".txt")) {
                return lerCsv(in.readAllBytes());
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof RegraException r) throw r;
            throw new RegraException("Não foi possível ler a planilha. Confira se o arquivo abre normalmente no Excel.");
        }
        throw new RegraException("Envie a planilha em .xlsx ou .csv. Arquivos .xls antigos: abra no Excel e salve como .xlsx.");
    }

    private static List<List<String>> lerXlsx(InputStream in) throws IOException {
        List<List<String>> out = new ArrayList<>();
        DataFormatter fmt = new DataFormatter(Locale.of("pt", "BR"));
        DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sh = wb.getSheetAt(0);
            for (Row r : sh) {
                List<String> linha = new ArrayList<>();
                for (int i = 0; i < Math.max(0, r.getLastCellNum()); i++) {
                    Cell c = r.getCell(i);
                    if (c == null) {
                        linha.add("");
                    } else if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                        linha.add(c.getLocalDateTimeCellValue().toLocalDate().format(br));
                    } else if (c.getCellType() == CellType.NUMERIC) {
                        double v = c.getNumericCellValue();
                        linha.add(v == Math.rint(v) ? String.valueOf((long) v) : fmt.formatCellValue(c));
                    } else {
                        linha.add(fmt.formatCellValue(c).trim());
                    }
                }
                if (linha.stream().anyMatch(s -> !s.isBlank())) {
                    out.add(linha);
                }
            }
        }
        return out;
    }

    static List<List<String>> lerCsv(byte[] bytes) {
        String txt;
        try {
            txt = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            txt = new String(bytes, Charset.forName("windows-1252"));
        }
        if (txt.startsWith("﻿")) {
            txt = txt.substring(1);
        }
        String primeira = txt.lines().findFirst().orElse("");
        char sep = primeira.chars().filter(ch -> ch == ';').count() >= primeira.chars().filter(ch -> ch == ',').count() ? ';' : ',';
        List<List<String>> out = new ArrayList<>();
        List<String> linha = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < txt.length(); i++) {
            char ch = txt.charAt(i);
            if (aspas) {
                if (ch == '"' && i + 1 < txt.length() && txt.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (ch == '"') aspas = false;
                else cur.append(ch);
            } else if (ch == '"') {
                aspas = true;
            } else if (ch == sep) {
                linha.add(cur.toString().trim());
                cur.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < txt.length() && txt.charAt(i + 1) == '\n') i++;
                linha.add(cur.toString().trim());
                cur.setLength(0);
                if (linha.stream().anyMatch(s -> !s.isBlank())) out.add(linha);
                linha = new ArrayList<>();
            } else {
                cur.append(ch);
            }
        }
        linha.add(cur.toString().trim());
        if (linha.stream().anyMatch(s -> !s.isBlank())) out.add(linha);
        return out;
    }
}
