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
        BigDecimal honorariosRecebidos) {

    public record DaeInfo(StatusDae status, BigDecimal venda, BigDecimal valor) {
    }
}
