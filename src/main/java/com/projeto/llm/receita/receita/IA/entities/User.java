package com.projeto.llm.receita.receita.IA.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "usuario")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "ultima_receita_criada")
    private String lastRecipeCreated;


    @Column(name = "numero_requisicoes", nullable = false)
    @Builder.Default
    private Integer requestCount = 0;

    @Column(name = "data_ultima_requisicao")
    private LocalDateTime lastRequestDate;

    @Column(name = "admin", nullable = false)
    @Builder.Default
    private Boolean admin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
