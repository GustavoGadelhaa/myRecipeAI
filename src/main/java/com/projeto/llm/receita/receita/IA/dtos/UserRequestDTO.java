package com.projeto.llm.receita.receita.IA.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class UserRequestDTO {

    @NotEmpty
    List<String> ingredientsList;
    @NotBlank(message = "Email cannot be null")
    @Email(message = "Email must be valid")
    String email;
    @JsonIgnore
    LocalDateTime time = LocalDateTime.now();
}
