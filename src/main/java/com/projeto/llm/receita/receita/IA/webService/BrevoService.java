package com.projeto.llm.receita.receita.IA.webService;

import kong.unirest.Unirest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kong.unirest.HttpResponse;


@RequiredArgsConstructor
@Service
public class BrevoService {

    @Value("${brevo.api.key}")
    String brevoKey;

    public void sendEmail(String html, String email) {

        String safeHtml = html
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");

        String body = String.format(
                "{\"sender\":{\"name\":\"Receita.IA \",\"email\":\"gustavogads4@gmail.com\"}," +
                        "\"to\":[{\"email\":\"%s\",\"name\":\"Destinatário\"}]," +
                        "\"subject\":\"Bora colocar a mão na massa?🍽️\"," +
                        "\"htmlContent\":\"%s\"}",
                email, safeHtml
        );

            HttpResponse<String> brevoResponse= Unirest.post("https://api.brevo.com/v3/smtp/email")
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("api-key", brevoKey)
                .body(body)
                .asString();

    }

    public void sendEmailToAdm(String admin) {
        String htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa; border-radius: 10px;">
                <div style="background-color: #dc3545; color: white; padding: 15px; border-radius: 8px 8px 0 0; text-align: center;">
                    <h2 style="margin: 0;">⚠️ ALERTA DO SISTEMA</h2>
                </div>
                <div style="background-color: white; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h3 style="color: #dc3545; margin-top: 0;">Tokens do Gemini Esgotados</h3>
                    <p style="font-size: 16px; line-height: 1.6; color: #333;">
                        O sistema <strong>Receita.IA</strong> está sem tokens disponíveis no Gemini API.
                    </p>
                    <p style="font-size: 16px; line-height: 1.6; color: #333;">
                        <strong>Ação necessária:</strong> Recarregue os créditos da API ou verifique o limite de uso.
                    </p>
                    <div style="margin: 25px 0; padding: 15px; background-color: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px;">
                        <p style="margin: 0; color: #856404; font-size: 14px;">
                            <strong>⚠️ Aviso:</strong> As funcionalidades de geração de receitas estão temporariamente indisponíveis.
                        </p>
                    </div>
                    <p style="font-size: 14px; color: #666; margin-top: 30px;">
                        Atenciosamente,<br>
                        <strong>Sistema Receita.IA</strong>
                    </p>
                </div>
            </div>
            """;

        String safeHtml = htmlContent
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");

        String body = String.format(
                "{\"sender\":{\"name\":\"Sistema Receita.IA\",\"email\":\"gustavogads4@gmail.com\"}," +
                        "\"to\":[{\"email\":\"%s\",\"name\":\"Administrador\"}]," +
                        "\"subject\":\"⚠️ URGENTE: Tokens do Gemini Esgotados\"," +
                        "\"htmlContent\":\"%s\"}",
                admin, safeHtml
        );

        HttpResponse<String> brevoResponse = Unirest.post("https://api.brevo.com/v3/smtp/email")
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("api-key", brevoKey)
                .body(body)
                .asString();
    }



}
