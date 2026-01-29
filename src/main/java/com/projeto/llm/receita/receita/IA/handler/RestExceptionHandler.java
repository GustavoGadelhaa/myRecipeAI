package com.projeto.llm.receita.receita.IA.handler;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.ClientException;
import com.projeto.llm.receita.receita.IA.dtos.UserResponseDTO;
import com.projeto.llm.receita.receita.IA.handler.exception.BusinessException;
import com.projeto.llm.receita.receita.IA.notificationAdm.NotificationAdm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
@RequiredArgsConstructor
@ControllerAdvice

public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    private final NotificationAdm notificationAdm;

    @ExceptionHandler(ClientException.class)
    private ResponseEntity<String>geminiTokensHandler(ClientException exception){
        notificationAdm.sendAlertToAdm();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Os tokens do nosso serviço de IA esgotaram, por favor tente mais tarde.");
    }

    @ExceptionHandler(ApiException.class)
    private ResponseEntity<String>brevoRequestLimit(ApiException exception){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Os tokens do nosso serviço de email esgotaram por hoje, por favor tente amanhã");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<UserResponseDTO> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new UserResponseDTO(ex.getMessage()));
    }



}
