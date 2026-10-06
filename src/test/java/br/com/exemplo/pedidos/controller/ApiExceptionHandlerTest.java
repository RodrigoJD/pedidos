package br.com.exemplo.pedidos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void deveRetornarBadRequestParaErroDeValidacao() {
        var target = new Object();
        var bindingResult = new BeanPropertyBindingResult(target, "pedido");
        bindingResult.addError(new FieldError("pedido", "produto", "não pode estar vazio"));
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.validation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("erro", "produto: valor inválido"), response.getBody());
    }

    @Test
    void deveRetornarBadRequestGenericoQuandoNaoHouverCampoComErro() {
        var target = new Object();
        var bindingResult = new BeanPropertyBindingResult(target, "pedido");
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.validation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("erro", "Payload inválido"), response.getBody());
    }

    @Test
    void deveRetornarInternalServerErrorParaExcecaoGenerica() {
        var response = handler.generic(new RuntimeException("erro"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                Map.of("erro", "Não foi possível processar a requisição"),
                response.getBody()
        );
    }
}
