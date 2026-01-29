package com.projeto.llm.receita.receita.IA.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.projeto.llm.receita.receita.IA.dtos.UserRequestDTO;
import com.projeto.llm.receita.receita.IA.dtos.GeminiResponseDTO;
import com.projeto.llm.receita.receita.IA.dtos.UserResponseDTO;
import com.projeto.llm.receita.receita.IA.handler.exception.BusinessException;
import com.projeto.llm.receita.receita.IA.repository.UserRepository;
import com.projeto.llm.receita.receita.IA.webService.BrevoService;
import com.projeto.llm.receita.receita.IA.webService.GeminiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceipeService {
    private final GeminiRequest geminiRequest;
    private final BrevoService brevoService;
    private final UserRepository userRepository;

    public UserResponseDTO returnReceipeIaInEmail(UserRequestDTO receipeAiRequestDTO) {

        GeminiResponseDTO responseDTO = new GeminiResponseDTO();
        UserResponseDTO userResponseDTO = new UserResponseDTO();


        boolean userExists = userRepository.existsByEmail(receipeAiRequestDTO.getEmail());
        if (!userExists) {
            userRepository.addEmail(receipeAiRequestDTO.getEmail());
        }
        boolean userIsAdm = userRepository.isAdmin(receipeAiRequestDTO.getEmail());


        LocalDateTime lastRequest = userRepository.getLastRequestDateByEmail(receipeAiRequestDTO.getEmail());


        if ((has24HoursPassedSince(lastRequest)) && !userIsAdm) {
            userRepository.resetRequestCount(receipeAiRequestDTO.getEmail());
        }

        Integer requestCount = userRepository.findRequestCountByEmail(receipeAiRequestDTO.getEmail());


        if (requestCount >= 3 && !userIsAdm) {
            throw new BusinessException(
                    "Tente novamente após 24 horas. Você atingiu o número máximo de requisições para este email: " + receipeAiRequestDTO.getEmail()
            );
        }


        userRepository.incrementRequestCount(receipeAiRequestDTO.getEmail(), 1);
        userRepository.updateLastRequestDate(receipeAiRequestDTO.getEmail(), LocalDateTime.now());


        receipeAiRequestDTO.setTime(LocalDateTime.now());
        ZonedDateTime nowBR = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        receipeAiRequestDTO.setTime(nowBR.toLocalDateTime());


        String prompt = String.valueOf(
                generatePrompt(receipeAiRequestDTO.getIngredientsList(), nowBR)
        );

        String aiReturn = geminiRequest.sendAiRequest(prompt);

        responseDTO = deserializeResponse(aiReturn);

        responseDTO.setTutorial(
                formatResponseText(responseDTO.getTutorial())
        );

        String emailHtml = generateEmail(
                receipeAiRequestDTO.getEmail(),
                responseDTO.getReceipeName(),
                responseDTO.getTimeToMake(),
                responseDTO.getDifficultyLevel(),
                responseDTO.getTutorial()
        );

        userRepository.updateLastRecipeCreated(receipeAiRequestDTO.getEmail(), responseDTO.getReceipeName());
        brevoService.sendEmail(emailHtml, receipeAiRequestDTO.getEmail());

        userResponseDTO.setMessageToUser(
                "Seu pedido foi um sucesso, confira seu email " + receipeAiRequestDTO.getEmail());
        return userResponseDTO;
    }


    private String formatResponseText(String text) {
        return text
                .replace("\\n", "\n")
                .replace("**", "");
    }

    public boolean has24HoursPassedSince(LocalDateTime lastRequestDate) {
        if (lastRequestDate == null) return true;
        return lastRequestDate.plusHours(24).isBefore(LocalDateTime.now());
    }


    public String generatePrompt(List<String> ingredients, ZonedDateTime mealTime) {
        String ingredientsFormatted = String.join(", ", ingredients);
        String mealType = determineMealType(mealTime);

        return """
        Crie APENAS UMA receita seguindo rigorosamente as regras abaixo:
        
        - Seja rapido na resposta, preciso de um retorno
        - Utilize SOMENTE os seguintes ingredientes: %s
        - NÃO adicione ingredientes extras
        - A receita deve ser adequada para o horário da refeição: %s (%s)
        - Gere APENAS UMA receita (sem variações)
        
        CRÍTICO: Retorne SOMENTE o JSON puro, sem ```json, sem ```, sem texto antes ou depois, sem explicações.
        
        Estrutura JSON obrigatória:
        {
          "receipeName": "Nome completo e atrativo da receita - %s",
          "tutorial": "Modo de preparo detalhado com instruções passo a passo",
          "difficultyLevel": número de 1 a 5 (1=muito fácil, 5=muito difícil),
          "timeToMake": "tempo no formato HH:MM (exemplo: 0:15 para 15 minutos, 1:00 para 1 hora, 1:30 para 1h30)"
        }
        
        REGRAS CRÍTICAS DE FORMATAÇÃO:
        
        1. Campo "receipeName":
           - Contém o nome da receita seguido do tipo de refeição (%s)
           - Formato: "Nome da Receita - %s"
           - Exemplo: "Omelete Simples de Queijo - Café da Manhã"
        
        2. Campo "tutorial":
           - Contém APENAS o modo de preparo em passos numerados
           - Use numeração ordinal (1º, 2º, 3º, 4º, 5º, etc.)
           - OBRIGATÓRIO: Separe cada passo com \n (quebra de linha JSON padrão)
           - Formato: "1º : Passo um\n2º : Passo dois\n3º : Passo três"
        
        3. Campo "difficultyLevel":
           - Número inteiro de 1 a 5 (sem aspas)
        
        4. Campo "timeToMake":
           - Formato: HH:MM seguido de "minutes" ou "hours"
        
        Exemplo de resposta válida:
        {
          "receipeName": "Omelete Simples de Queijo - Café da Manhã",
          "tutorial": "1º : Quebre os ovos em uma tigela e bata bem\n2º : Aqueça uma frigideira com azeite\n3º : Despeje os ovos batidos\n4º : Adicione o queijo ralado\n5º : Dobre ao meio quando firme\n6º : Sirva quente",
          "difficultyLevel": 1,
          "timeToMake": "0:15 minutes"
        }
        
        VALIDAÇÃO FINAL:
        - Retorne APENAS o objeto JSON válido
        - Use \n (quebra de linha JSON) para separar os passos""".formatted(
                ingredientsFormatted,
                mealTime,
                mealType,
                mealType,
                mealType,
                mealType
        );
    }

    private String determineMealType(ZonedDateTime mealTime) {
        int hour = mealTime.getHour();

        if (hour >= 6 && hour < 11) {
            return "Café da Manhã";
        } else if (hour >= 11 && hour < 15) {
            return "Almoço";
        } else if (hour >= 15 && hour < 18) {
            return "Lanche";
        } else {
            return "Janta";
        }
    }

    public GeminiResponseDTO deserializeResponse(String jsonResponse) {
        try {
            String cleanJson = jsonResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Duration.class,
                            (JsonDeserializer<Duration>) (json, type, context) ->
                                    Duration.parse(json.getAsString()))
                    .create();

            return gson.fromJson(cleanJson, GeminiResponseDTO.class);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao desserializar resposta da IA: " + e.getMessage(), e);
        }
    }

    public String generateEmail(
            String userEmail,
            String recipeName,
            String time,
            int difficulty,
            String recipeText
    ) {
        String stars = getDifficultyText(difficulty);
        String difficultyColor = getDifficultyColor(difficulty);

        // Converte o texto da receita em parágrafos HTML com máximo contraste
        String recipeHtml = Arrays.stream(recipeText.split("\n\n"))
                .map(paragraph -> "<p style=\"margin: 0 0 15px 0 !important; color: #000000 !important; -webkit-text-fill-color: #000000 !important; background-color: #f5f5f5 !important; padding: 12px !important; border-radius: 8px !important; font-size: 15px !important; line-height: 1.7 !important;\">"
                        + "<span style=\"color: #000000 !important;\">" + paragraph.replace("\n", "<br>") + "</span></p>")
                .collect(Collectors.joining(""));

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta name="color-scheme" content="light only">
                <meta name="supported-color-schemes" content="light">
                <meta name="x-apple-disable-message-reformatting">
                <title>Sua Receita Personalizada</title>
                <style>
                    /* Força modo claro no iOS */
                    :root {
                        color-scheme: light only !important;
                        supported-color-schemes: light !important;
                    }
                    body {
                        background-color: #b35220 !important;
                    }
                    /* Previne modo escuro */
                    @media (prefers-color-scheme: dark) {
                        body, table, td, p, div, span, h1, h2, h3 {
                            color: #000000 !important;
                            background-color: #ffffff !important;
                        }
                        .header-gradient {
                            background: linear-gradient(135deg, #c54a3a 0%%, #e6673d 50%%, #d88236 100%%) !important;
                        }
                        .white-text {
                            color: #ffffff !important;
                        }
                    }
                </style>
            </head>
            <body style="margin: 0 !important; padding: 0 !important; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif !important; background-color: #b35220 !important; min-height: 100vh; padding: 40px 20px !important; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;">
            
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: #ffffff !important; border-radius: 24px; overflow: hidden; box-shadow: 0 25px 70px rgba(0,0,0,0.6);">
                    <tr>
                        <td style="padding: 0; background-color: #ffffff !important;">
            
                            <!-- HEADER -->
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" class="header-gradient" style="background: linear-gradient(135deg, #c54a3a 0%%, #e6673d 50%%, #d88236 100%%) !important;">
                                <tr>
                                    <td style="padding: 60px 40px; text-align: center;">
                                        <h1 class="white-text" style="margin: 0 !important; color: #ffffff !important; -webkit-text-fill-color: #ffffff !important; font-size: 36px !important; font-weight: 800 !important; letter-spacing: -1px !important; text-shadow: 0 2px 20px rgba(0,0,0,0.4) !important;">
                                            <span style="color: #ffffff !important;">Sua Receita Está Pronta! 🍽️</span>
                                        </h1>
                                        <p class="white-text" style="margin: 15px 0 0 0 !important; color: #ffffff !important; -webkit-text-fill-color: #ffffff !important; font-size: 17px !important; font-weight: 500 !important;">
                                            <span style="color: #ffffff !important;">Preparamos algo especial para você ✨</span>
                                        </p>
                                    </td>
                                </tr>
                            </table>
            
                            <!-- CONTENT -->
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background-color: #ffffff !important;">
                                <tr>
                                    <td style="padding: 50px 40px; background-color: #ffffff !important;">
            
                                        <p style="margin: 0 0 20px 0 !important; color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 17px !important; line-height: 1.6 !important;">
                                            <span style="color: #000000 !important;">Olá, <strong style="color: #e6673d !important;">%s</strong>! 👋</span>
                                        </p>
            
                                        <p style="margin: 0 0 30px 0 !important; color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 16px !important; line-height: 1.7 !important;">
                                            <span style="color: #000000 !important;">Criamos uma receita deliciosa e personalizada especialmente para você. Está pronto para colocar a mão na massa?</span>
                                        </p>
            
                                        <!-- RECIPE INFO BOX -->
                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background: linear-gradient(135deg, #fff5f0 0%%, #ffe8dc 100%%) !important; border-radius: 18px; margin: 30px 0; border: 3px solid #e6673d !important;">
                                            <tr>
                                                <td style="padding: 35px; background-color: #fff5f0 !important;">
                                                    <h2 style="margin: 0 0 25px 0 !important; color: #c54a3a !important; -webkit-text-fill-color: #c54a3a !important; font-size: 26px !important; font-weight: 700 !important;">
                                                        <span style="color: #c54a3a !important;">🍳 %s</span>
                                                    </h2>
            
                                                  <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%">
                                                    <tr>
                                                        <td style="padding: 15px 20px 15px 0; width: 50%%; border-right: 2px solid #e6673d;">
                                                            <div style="color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 13px !important; margin-bottom: 8px !important; text-transform: uppercase !important; letter-spacing: 1px !important; font-weight: 600 !important;">
                                                                <span style="color: #000000 !important;">⏱️ Tempo de Preparo</span>
                                                            </div>
                                                            <div style="color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 18px !important; font-weight: 700 !important;">
                                                                <span style="color: #000000 !important;">%s</span>
                                                            </div>
                                                        </td>
                                                        <td style="padding: 15px 0 15px 20px; width: 50%%;">
                                                            <div style="color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 13px !important; margin-bottom: 8px !important; text-transform: uppercase !important; letter-spacing: 1px !important; font-weight: 600 !important;">
                                                                <span style="color: #000000 !important;">📊 Dificuldade</span>
                                                            </div>
                                                            <div style="color: %s !important; -webkit-text-fill-color: %s !important; font-size: 18px !important; font-weight: 700 !important;">
                                                                <span style="color: %s !important;">%s</span>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </table>
                                                </td>
                                            </tr>
                                        </table>
            
                                        <!-- RECIPE STEPS -->
                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="margin: 40px 0; background-color: #f9f9f9 !important; border-radius: 18px; border-left: 6px solid #e6673d !important;">
                                            <tr>
                                                <td style="padding: 35px; background-color: #f9f9f9 !important;">
                                                    <h3 style="margin: 0 0 25px 0 !important; color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 22px !important; font-weight: 700 !important;">
                                                        <span style="color: #000000 !important;">📝 Modo de Preparo</span>
                                                    </h3>
                                                    <div>
                                                        %s
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
            
                                        <!-- FEATURES -->
                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="margin: 40px 0;">
                                            <tr>
                                                <td style="padding: 20px; text-align: center; width: 30%%; background-color: #fff5f0 !important; border-radius: 15px; border: 2px solid #e6673d;">
                                                    <div style="font-size: 38px; margin-bottom: 12px;">🥘</div>
                                                    <div style="color: #c54a3a !important; -webkit-text-fill-color: #c54a3a !important; font-size: 14px !important; font-weight: 700 !important; text-transform: uppercase !important;">
                                                        <span style="color: #c54a3a !important;">Saborosa</span>
                                                    </div>
                                                </td>
                                                <td style="width: 5%%;"></td>
                                                <td style="padding: 20px; text-align: center; width: 30%%; background-color: #fff5f0 !important; border-radius: 15px; border: 2px solid #d88236;">
                                                    <div style="font-size: 38px; margin-bottom: 12px;">✨</div>
                                                    <div style="color: #d88236 !important; -webkit-text-fill-color: #d88236 !important; font-size: 14px !important; font-weight: 700 !important; text-transform: uppercase !important;">
                                                        <span style="color: #d88236 !important;">Testada</span>
                                                    </div>
                                                </td>
                                                <td style="width: 5%%;"></td>
                                                <td style="padding: 20px; text-align: center; width: 30%%; background-color: #fff5f0 !important; border-radius: 15px; border: 2px solid #c54a3a;">
                                                    <div style="font-size: 38px; margin-bottom: 12px;">❤️</div>
                                                    <div style="color: #c54a3a !important; -webkit-text-fill-color: #c54a3a !important; font-size: 14px !important; font-weight: 700 !important; text-transform: uppercase !important;">
                                                        <span style="color: #c54a3a !important;">Com Amor</span>
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
            
                                        <!-- CTA BOX -->
                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background-color: #fffbf5 !important; border-radius: 15px; margin: 35px 0; border: 2px solid #d88236 !important;">
                                            <tr>
                                                <td style="padding: 30px; text-align: center; background-color: #fffbf5 !important;">
                                                    <p style="margin: 0 !important; color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 15px !important; line-height: 1.6 !important;">
                                                        <span style="color: #000000 !important;">💡 <strong style="color: #d88236 !important;">Dica Importante:</strong> Leia toda a receita antes de começar e separe todos os ingredientes. Isso vai facilitar muito o processo!</span>
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
            
                                   <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="margin: 35px 0; background: linear-gradient(135deg, #fff5f0 0%%, #ffe8dc 100%%) !important; border-radius: 15px; border: 2px solid #e6673d !important;">
                                       <tr>
                                           <td style="padding: 25px; text-align: center; background-color: #fff5f0 !important;">
                                               <p style="margin: 0 0 10px 0 !important; color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 16px !important; line-height: 1.6 !important;">
                                                   <span style="color: #000000 !important;">Esperamos que você aproveite muito essa receita! 😊</span>
                                               </p>
                                               <p style="margin: 0 !important; color: #000000 !important; -webkit-text-fill-color: #000000 !important; font-size: 15px !important;">
                                                   <span style="color: #000000 !important;">Bom apetite e boa diversão na cozinha! 🎉</span>
                                               </p>
                                           </td>
                                       </tr>
                                   </table>
            
                                        <p style="margin: 30px 0 0 0 !important; color: #666666 !important; -webkit-text-fill-color: #666666 !important; font-size: 13px !important; text-align: center !important; font-style: italic !important;">
                                            <span style="color: #666666 !important;">Developed by: <a href="https://www.linkedin.com/in/gustavogadelhadev/" target="_blank" style="color: #e6673d !important; text-decoration: none !important; font-weight: 700 !important;"><span style="color: #e6673d !important;">Gustavo Gadelha 💻</span></a></span>
                                        </p>
                                    </td>
                                </tr>
                            </table>
            
                            <!-- FOOTER -->
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background-color: #f5f5f5 !important;">
                                <tr>
                                    <td style="padding: 35px 40px; background-color: #f5f5f5 !important; border-top: 2px solid #e6673d;">
                                        <p style="margin: 0 0 12px 0 !important; color: #666666 !important; -webkit-text-fill-color: #666666 !important; font-size: 13px !important; text-align: center !important; font-weight: 600 !important;">
                                            <span style="color: #666666 !important;">©️ 2026 Receita.IA - <a href="https://github.com/GustavoGadelhaa/myRecipeAI" target="_blank" style="color: #e6673d !important; text-decoration: none !important; font-weight: 700 !important;"><span style="color: #e6673d !important;">Link do Repositório</span></a></span>
                                        </p>
                                        <p style="margin: 0 !important; color: #888888 !important; -webkit-text-fill-color: #888888 !important; font-size: 12px !important; text-align: center !important;">
                                            <span style="color: #888888 !important;">Receita gerada automaticamente com inteligência artificial 🧠</span>
                                        </p>
                                    </td>
                                </tr>
                            </table>
            
                        </td>
                    </tr>
                </table>
            
            </body>
            </html>
            """.formatted(
                userEmail,
                recipeName,
                time,
                difficultyColor,
                difficultyColor,
                difficultyColor,
                stars,
                recipeHtml
        );
    }

    private String getDifficultyText(int level) {
        return switch (level) {
            case 1 -> "⭐ Muito Fácil";
            case 2 -> "⭐⭐ Fácil";
            case 3 -> "⭐⭐⭐ Médio";
            case 4 -> "⭐⭐⭐⭐ Difícil";
            case 5 -> "⭐⭐⭐⭐⭐ Muito Difícil";
            default -> "⭐⭐⭐ Médio";
        };
    }


    private String getDifficultyColor(int level) {
        return switch (level) {
            case 1 -> "#27ae60";
            case 2 -> "#2ecc71";
            case 3 -> "#f39c12";
            case 4 -> "#e67e22";
            case 5 -> "#e74c3c";
            default -> "#f39c12";
        };
    }
}