package br.com.segurodefeso.servico;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.segurodefeso.dominio.ConfigValores;
import br.com.segurodefeso.entidade.ConfigItem;
import br.com.segurodefeso.repositorio.ConfigRepo;

@Service
public class ConfigService {

    private final ConfigRepo repo;

    public ConfigService(ConfigRepo repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public ConfigValores valores() {
        ConfigValores p = ConfigValores.padrao();
        Map<String, String> m = new HashMap<>();
        repo.findAll().forEach(i -> m.put(i.getChave(), i.getValor()));
        return new ConfigValores(
            dec(m.get("salario_minimo"), p.salarioMinimo()),
            dec(m.get("honorarios_pct"), p.honorariosPct()),
            dec(m.get("aliquota_dae"), p.aliquotaDae()),
            inteiro(m.get("dia_vencimento_dae"), p.diaVencimentoDae()),
            inteiro(m.get("parcelas_padrao"), p.parcelasPadrao()),
            m.getOrDefault("nome_escritorio", p.nomeEscritorio()));
    }

    @Transactional
    public ConfigValores salvar(ConfigValores v) {
        if (v.salarioMinimo() == null || v.salarioMinimo().signum() <= 0) {
            throw new RegraException("Informe o salário mínimo vigente.");
        }
        if (v.honorariosPct() == null || v.honorariosPct().signum() < 0 || v.honorariosPct().compareTo(new BigDecimal("100")) > 0) {
            throw new RegraException("Honorários: use um percentual entre 0 e 100.");
        }
        if (v.aliquotaDae() == null || v.aliquotaDae().signum() < 0) {
            throw new RegraException("Informe a alíquota do DAE.");
        }
        if (v.diaVencimentoDae() < 1 || v.diaVencimentoDae() > 28) {
            throw new RegraException("Dia de vencimento do DAE: use um número de 1 a 28.");
        }
        if (v.parcelasPadrao() < 1 || v.parcelasPadrao() > 12) {
            throw new RegraException("Parcelas padrão: use um número de 1 a 12.");
        }
        repo.save(new ConfigItem("salario_minimo", v.salarioMinimo().toPlainString()));
        repo.save(new ConfigItem("honorarios_pct", v.honorariosPct().toPlainString()));
        repo.save(new ConfigItem("aliquota_dae", v.aliquotaDae().toPlainString()));
        repo.save(new ConfigItem("dia_vencimento_dae", String.valueOf(v.diaVencimentoDae())));
        repo.save(new ConfigItem("parcelas_padrao", String.valueOf(v.parcelasPadrao())));
        if (v.nomeEscritorio() != null && !v.nomeEscritorio().isBlank()) {
            repo.save(new ConfigItem("nome_escritorio", v.nomeEscritorio().trim()));
        }
        return valores();
    }

    private static BigDecimal dec(String s, BigDecimal padrao) {
        try {
            return s == null ? padrao : new BigDecimal(s);
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    private static int inteiro(String s, int padrao) {
        try {
            return s == null ? padrao : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
}
