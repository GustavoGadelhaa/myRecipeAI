package com.projeto.llm.receita.receita.IA.controller;

import com.projeto.llm.receita.receita.IA.dtos.UserRequestDTO;
import com.projeto.llm.receita.receita.IA.dtos.UserResponseDTO;
import com.projeto.llm.receita.receita.IA.service.ReceipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/receipe")
@RequiredArgsConstructor
@Slf4j
public class ReceipeController {

    private final ReceipeService receipeService;

    @PostMapping
    public ResponseEntity<UserResponseDTO>getReceipe(@Valid @RequestBody UserRequestDTO requestDTO){
        log.info("{} fez uma request às {}", requestDTO.getEmail(), ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        UserResponseDTO response = receipeService.returnReceipeIaInEmail(requestDTO);
        return ResponseEntity.ok(response);

    }


}
