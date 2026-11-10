package br.com.segurodefeso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Retrato de um cliente com tudo o que as regras precisam.
 * É montado pelo serviço a partir do banco; as regras não dependem do Spring nem do banco.
 */
public record ClienteDados(
        Long id,
        String nome,
        String cpf,
        String nit,
        LocalDate nascimento,
        String telefone,
        String endereco,
        String municipio,
        String colonia,
        String rgpNumero,
        String rgpDesde,
        LocalDate rgpValidade,
        String especie,
        LocalDate procuracaoValidade,
        String govNivel,
        BigDecimal honorariosPct,
        Etapa etapa,
        String protocolo,
        String situacao,
        String exigenciaTexto,
        LocalDate prazoData,
        String indeferimentoMotivo,
        LocalDate cienciaData,
        int parcelasTotal,
        int parcelasLiberadas,
        LocalDate ultimaConferencia,
        String observacoes,
        boolean ativo,
        /* requisito -> true (atende), false (não atende); ausente = sem resposta */
        Map<Requisito, Boolean> requisitos,
        Set<TipoDocumento> documentos,
        /* "AAAA-MM" -> lançamento do DAE */
        Map<String, DaeInfo> daes,
        BigDecimal honorariosRecebidos,
        Entrevista entrevista,
        Pessoal pessoal) {

    public record DaeInfo(StatusDae status, BigDecimal venda, BigDecimal valor) {
    }

    /** Dados cadastrais complementares. A senha gov nunca viaja aqui: só se há uma cadastrada. */
    public record Pessoal(String sexo, String rg, String orgaoEmissor, LocalDate dataExpedicao, String nomePai,
                          String nomeMae, String estadoCivil, String estadoOrigem, String cidadeOrigem,
                          String tituloEleitor, String inscricaoNumero, LocalDate dataAfiliacao, String cep,
                          String logradouro, String numero, String complemento, String bairro, String uf,
                          Long auxiliarId, boolean senhaGovCadastrada) {

        public static Pessoal vazio() {
            return new Pessoal(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, false);
        }
    }
}
