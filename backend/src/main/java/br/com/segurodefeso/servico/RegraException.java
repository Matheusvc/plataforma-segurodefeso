package br.com.segurodefeso.servico;

/** Erro de regra de negócio ou de preenchimento: vira resposta 400 com a mensagem para o usuário. */
public class RegraException extends RuntimeException {
    public RegraException(String mensagem) {
        super(mensagem);
    }
}
