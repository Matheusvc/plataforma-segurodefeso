package br.com.segurodefeso.dominio;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Modelos de mensagem de WhatsApp preenchidos com os dados do cliente. */
public final class Mensagens {

    public enum Modelo {
        DAE("DAE pronto para pagamento"),
        DOCS("Documentos pendentes"),
        EXIGENCIA("Exigência no pedido"),
        LOGIN("Agendar acesso ao gov.br"),
        DEFERIDO("Pedido deferido"),
        INDEFERIDO("Pedido indeferido"),
        PROCURACAO("Renovar procuração e-CAC");

        private final String nome;

        Modelo(String nome) {
            this.nome = nome;
        }

        public String getNome() { return nome; }
    }

    private Mensagens() {
    }

    public static String texto(Modelo modelo, Analise a, ConfigValores cfg) {
        ClienteDados c = a.cliente();
        String pn = c.nome() == null ? "" : c.nome().split(" ")[0];
        String esc = cfg.nomeEscritorio();
        return switch (modelo) {
            case DAE -> {
                List<String> linhas = new ArrayList<>();
                List<Analise.DaeAberto> todos = new ArrayList<>(a.daeVencidos());
                todos.addAll(a.daeAbertos());
                for (Analise.DaeAberto d : todos) {
                    ClienteDados.DaeInfo info = c.daes().get(d.competencia());
                    String valor = info != null && info.valor() != null ? Dinheiro.brl(info.valor()) : "[valor da guia]";
                    linhas.add("• " + d.label() + ": " + valor + (a.daeVencidos().contains(d) ? " (vencido)" : ""));
                }
                String lista = linhas.isEmpty() ? "• Nenhuma guia em aberto no momento." : String.join("\n", linhas);
                yield "Olá, " + pn + "! Aqui é do " + esc + ".\nSeguem as guias DAE em aberto:\n" + lista + "\n"
                        + "Pague até o vencimento que está na guia e mande a foto do comprovante por aqui.\n"
                        + "Sem esse pagamento, o seu seguro-defeso pode ser negado.";
            }
            case DOCS -> {
                String pend = a.documentosPendentes().isEmpty() ? "• Nenhum documento pendente"
                        : String.join("\n", a.documentosPendentes().stream().map(t -> "• " + t.getNome()).toList());
                yield "Olá, " + pn + "! Para seguir com o seu seguro-defeso, precisamos de:\n" + pend
                        + "\nPode mandar foto legível por aqui mesmo.";
            }
            case EXIGENCIA -> "Olá, " + pn + "! O seu pedido do seguro-defeso (protocolo " + Prazos.nvl(c.protocolo(), "[protocolo]")
                    + ") recebeu uma exigência:\n" + Prazos.nvl(c.exigenciaTexto(), "[o que foi pedido]") + "\nPrazo: "
                    + (c.prazoData() != null ? Datas.br(c.prazoData()) : "[prazo]") + ". Envie o documento por aqui o quanto antes.";
            case LOGIN -> "Olá, " + pn + "! Vamos fazer o seu pedido do seguro-defeso em [data], às [hora].\n"
                    + "Nesse horário, fique com o celular por perto: vamos precisar do código que o gov.br vai enviar para você.";
            case DEFERIDO -> "Olá, " + pn + "! Boa notícia: o seu seguro-defeso foi deferido.\n"
                    + "Vamos avisar a cada parcela liberada. Qualquer dúvida, chame por aqui.";
            case INDEFERIDO -> "Olá, " + pn + ". O seu pedido do seguro-defeso foi negado.\nMotivo informado: "
                    + Prazos.nvl(c.indeferimentoMotivo(), "[motivo]") + "\nJá estamos analisando o recurso. Precisamos conversar com você até [data].";
            case PROCURACAO -> "Olá, " + pn + "! A procuração eletrônica que você deu ao escritório no e-CAC vence em "
                    + (c.procuracaoValidade() != null ? Datas.br(c.procuracaoValidade()) : "[data]")
                    + ".\nPrecisamos renová-la para continuar emitindo o seu DAE. Podemos marcar um horário?";
        };
    }

    /** Link do WhatsApp com o texto pronto, ou null se o telefone não servir. */
    public static String linkWhatsapp(String telefone, String texto) {
        String wa = Telefone.whatsapp(telefone);
        if (wa == null || texto == null || texto.isBlank()) {
            return null;
        }
        return "https://wa.me/" + wa + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
