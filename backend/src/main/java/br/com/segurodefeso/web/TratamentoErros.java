package br.com.segurodefeso.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import br.com.segurodefeso.servico.NaoEncontradoException;
import br.com.segurodefeso.servico.RegraException;

/** Converte erros em respostas JSON {"erro": "mensagem"} que a tela mostra ao usuário. */
@RestControllerAdvice
public class TratamentoErros {

    @ExceptionHandler(RegraException.class)
    public ResponseEntity<Map<String, String>> regra(RegraException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(NaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> negado(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", "Você não tem permissão para esta ação."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> grande(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Arquivo muito grande. O limite é 15 MB."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Dados inválidos na requisição."));
    }
}
