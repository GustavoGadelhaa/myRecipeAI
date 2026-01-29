package com.projeto.llm.receita.receita.IA.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class GeminiResponseDTO {
    String tutorial;
    String receipeName;
    int difficultyLevel;
    String timeToMake;
}
