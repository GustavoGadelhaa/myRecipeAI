package com.projeto.llm.receita.receita.IA.controller;

import com.projeto.llm.receita.receita.IA.dtos.UserRequestDTO;
import com.projeto.llm.receita.receita.IA.dtos.UserResponseDTO;
import com.projeto.llm.receita.receita.IA.service.ReceipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/receipe")
@RequiredArgsConstructor
public class ReceipeController {

    private final ReceipeService receipeService;

    @PostMapping
    public ResponseEntity<UserResponseDTO>getReceipe(@Valid @RequestBody UserRequestDTO requestDTO){
        UserResponseDTO response = receipeService.returnReceipeIaInEmail(requestDTO);
        return ResponseEntity.ok(response);

    }


}
