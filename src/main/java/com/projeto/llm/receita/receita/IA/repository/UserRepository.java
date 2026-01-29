package com.projeto.llm.receita.receita.IA.repository;

import com.projeto.llm.receita.receita.IA.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO usuario (email, admin, numero_requisicoes, created_at) VALUES (?1, false, 0, NOW())", nativeQuery = true)
    void addEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastRecipeCreated = :lastRecipe WHERE u.email = :email")
    void updateLastRecipeCreated(@Param("email") String email, @Param("lastRecipe") String lastRecipe);


    @Query("SELECT u.requestCount FROM User u WHERE u.email = :email")
    Integer findRequestCountByEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.requestCount = u.requestCount + :increment WHERE u.email = :email")
    void incrementRequestCount(@Param("email") String email, @Param("increment") Integer increment);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastRequestDate = :date WHERE u.email = :email")
    void updateLastRequestDate(@Param("email") String email, @Param("date") LocalDateTime date);

    @Query("SELECT u.lastRequestDate FROM User u WHERE u.email = :email")
    LocalDateTime getLastRequestDateByEmail(@Param("email") String email);

    @Query("SELECT u.admin FROM User u WHERE u.email = :email")
    Boolean isAdmin(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.requestCount = 0 WHERE u.email = :email")
    void resetRequestCount(@Param("email") String email);





}