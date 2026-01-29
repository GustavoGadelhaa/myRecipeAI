package com.projeto.llm.receita.receita.IA.webService;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class GeminiRequest {

    public String sendAiRequest(String prompt){
        try {
            Client client = new Client();
            GenerateContentResponse responseGemini =
                    client.models.generateContent(
                            "gemini-2.5-flash-lite",
                            prompt,
                            null);
            return responseGemini.text();
        } catch (Exception e) {
            e.getStackTrace();
            throw new RuntimeException(e);
        }

    }

}
