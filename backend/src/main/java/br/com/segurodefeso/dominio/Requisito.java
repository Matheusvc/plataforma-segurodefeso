package br.com.segurodefeso.dominio;

/**
 * Requisitos do seguro-defeso conferidos na triagem.
 * Base: Lei 10.779/2003, com a redação da Lei 13.134/2015, e regulamento.
 */
public enum Requisito {
    RGP1ANO("RGP ativo, com registro há pelo menos 1 ano antes do início do defeso",
            "RGP com menos de 1 ano", "extrato do RGP com data de registro e situação ativa", true),
    HABITUAL("Pesca artesanal como profissão habitual e principal meio de vida",
            "pesca não é a atividade principal", "RGP, declaração da colônia, notas de venda", true),
    SEM_VINCULO("Sem vínculo de emprego ou outra fonte de renda fora da pesca",
            "vínculo ou renda fora da pesca", "CNIS sem vínculo ativo no período", true),
    SEM_BENEFICIO("Sem benefício continuado incompatível (exceto pensão por morte e auxílio-acidente)",
            "benefício continuado incompatível", "consulta de benefícios ativos no CNIS / Meu INSS", true),
    RECOLHIMENTO("Contribuição sobre a comercialização recolhida no período exigido",
            "contribuição (DAE) não recolhida", "DAEs pagos no eSocial", false),
    RESIDENCIA("Residência em município abrangido pela portaria do defeso",
            "residência não comprovada na área do defeso", "comprovante recente em nome do pescador ou declaração", false),
    GOV("Conta gov.br no nível exigido para o requerimento",
            "conta gov.br abaixo do nível exigido", "tela de nível da conta no app gov.br", false);

    public static final String BASE_LEGAL = "Requisitos com base na Lei 10.779/2003, com a redação da Lei 13.134/2015, "
            + "e no regulamento. Conferir a portaria do defeso da espécie e da região e a norma vigente antes de protocolar.";

    private final String texto;
    private final String curto;
    private final String prova;
    private final boolean essencial;

    Requisito(String texto, String curto, String prova, boolean essencial) {
        this.texto = texto;
        this.curto = curto;
        this.prova = prova;
        this.essencial = essencial;
    }

    public String getTexto() { return texto; }
    public String getCurto() { return curto; }
    public String getProva() { return prova; }
    public boolean isEssencial() { return essencial; }
}
